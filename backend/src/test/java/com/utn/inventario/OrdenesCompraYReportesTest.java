package com.utn.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdenesCompraYReportesTest extends ApiTestBase {

    private JsonNode crearOrden(long proveedor, List<Map<String, Object>> items) throws Exception {
        return cuerpo(post("/api/ordenes-compra", tokenEmpleado,
                Map.of("proveedorId", proveedor, "observaciones", "test", "items", items))
                .andExpect(status().isOk()));
    }

    @Test
    void crearUnaOrdenNoCambiaElStock() throws Exception {
        long proveedor = crearProveedor();
        long id = crearProducto(proveedor, null, "1500", "1000", 5, 10);

        JsonNode orden = crearOrden(proveedor, List.of(itemCompra(id, 20, "1100")));

        assertThat(orden.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(dinero(orden.get("total"))).isEqualByComparingTo("22000");
        assertThat(orden.get("fechaRecepcion").isNull()).isTrue();
        assertThat(stock(id)).isEqualTo(5);
        assertThat(movimientos(id)).isEmpty();
    }

    @Test
    void recibirSumaElStockYActualizaElCosto() throws Exception {
        long proveedor = crearProveedor();
        long a = crearProducto(proveedor, null, "1500", "1000", 5, 10);
        long b = crearProducto(proveedor, null, "300", "200", 0, 10);
        long orden = crearOrden(proveedor, List.of(itemCompra(a, 20, "1100"), itemCompra(b, 10, "180")))
                .get("id").asLong();

        post("/api/ordenes-compra/" + orden + "/recibir", tokenEmpleado, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.fechaRecepcion").isNotEmpty());

        JsonNode pa = producto(a);
        assertThat(pa.get("stock").asInt()).isEqualTo(25);
        assertThat(dinero(pa.get("precioCosto"))).isEqualByComparingTo("1100");
        assertThat(dinero(pa.get("margen"))).isEqualByComparingTo("36.4");
        assertThat(stock(b)).isEqualTo(10);
        assertThat(movimientos(a).get(0).get("motivo").asText()).startsWith("Orden de compra #" + orden);
    }

    @Test
    void unaOrdenSoloSeRecibeOCancelaUnaVez() throws Exception {
        long proveedor = crearProveedor();
        long id = crearProducto(proveedor, null, "10", "5", 0, 0);

        long recibida = crearOrden(proveedor, List.of(itemCompra(id, 5, "5"))).get("id").asLong();
        post("/api/ordenes-compra/" + recibida + "/recibir", tokenEmpleado, null).andExpect(status().isOk());
        post("/api/ordenes-compra/" + recibida + "/recibir", tokenEmpleado, null).andExpect(status().isBadRequest());
        post("/api/ordenes-compra/" + recibida + "/cancelar", tokenEmpleado, null).andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(5);

        long cancelada = crearOrden(proveedor, List.of(itemCompra(id, 5, "5"))).get("id").asLong();
        post("/api/ordenes-compra/" + cancelada + "/cancelar", tokenEmpleado, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        post("/api/ordenes-compra/" + cancelada + "/recibir", tokenEmpleado, null).andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(5);
    }

    @Test
    void ordenesInvalidasSeRechazan() throws Exception {
        long proveedor = crearProveedor();
        long id = crearProducto(proveedor, null, "10", "5", 0, 0);

        post("/api/ordenes-compra", tokenEmpleado, Map.of("items", List.of(itemCompra(id, 1, "5"))))
                .andExpect(status().isBadRequest());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", 999999, "items", List.of(itemCompra(id, 1, "5"))))
                .andExpect(status().isNotFound());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", proveedor, "items", List.of()))
                .andExpect(status().isBadRequest());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", proveedor,
                "items", List.of(itemCompra(id, 1, "5"), itemCompra(id, 2, "5"))))
                .andExpect(status().isBadRequest());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", proveedor,
                "items", List.of(itemCompra(id, 1, "-5"))))
                .andExpect(status().isBadRequest());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", proveedor,
                "items", List.of(itemCompra(id, 0, "5"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noSePidenProductosDadosDeBaja() throws Exception {
        long proveedor = crearProveedor();
        long id = crearProducto(proveedor, null, "10", "5", 0, 0);
        delete("/api/productos/" + id, tokenAdmin).andExpect(status().isNoContent());
        post("/api/ordenes-compra", tokenEmpleado, Map.of("proveedorId", proveedor,
                "items", List.of(itemCompra(id, 1, "5"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proveedorConOrdenesNoSeElimina() throws Exception {
        long proveedor = crearProveedor();
        long otroProveedor = crearProveedor();
        long id = crearProducto(otroProveedor, null, "10", "5", 0, 0);
        crearOrden(proveedor, List.of(itemCompra(id, 1, "5")));

        delete("/api/proveedores/" + proveedor, tokenAdmin).andExpect(status().isBadRequest());
    }

    @Test
    void ordenInexistenteDa404() throws Exception {
        get("/api/ordenes-compra/999999", tokenAdmin).andExpect(status().isNotFound());
        post("/api/ordenes-compra/999999/recibir", tokenAdmin, null).andExpect(status().isNotFound());
    }

    @Test
    void elReporteReflejaProductosVentasYOrdenes() throws Exception {
        JsonNode antes = reporte();

        long proveedor = crearProveedor();
        long id = crearProducto(proveedor, null, "100", "60", 10, 2);
        JsonNode conProducto = reporte();
        assertThat(conProducto.get("totalProductos").asLong()).isEqualTo(antes.get("totalProductos").asLong() + 1);
        assertDiferencia(antes, conProducto, "valorTotalInventario", "1000");
        assertDiferencia(antes, conProducto, "valorInventarioCosto", "600");
        assertDiferencia(antes, conProducto, "gananciaPotencial", "400");

        post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 3)))).andExpect(status().isOk());
        JsonNode conVenta = reporte();
        assertDiferencia(conProducto, conVenta, "ventasDelMes", "300");
        assertThat(conVenta.get("cantidadVentasDelMes").asLong())
                .isEqualTo(conProducto.get("cantidadVentasDelMes").asLong() + 1);
        assertDiferencia(conProducto, conVenta, "valorTotalInventario", "-300");

        long anulada = cuerpo(post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 1))))).get("id").asLong();
        post("/api/ventas/" + anulada + "/anular", tokenAdmin, null).andExpect(status().isOk());
        assertDiferencia(conVenta, reporte(), "ventasDelMes", "0");

        crearOrden(proveedor, List.of(itemCompra(id, 5, "60")));
        assertThat(reporte().get("ordenesCompraPendientes").asLong())
                .isEqualTo(conVenta.get("ordenesCompraPendientes").asLong() + 1);

        long agotado = crearProducto(null, null, "10", "5", 0, 3);
        JsonNode conAgotado = reporte();
        assertThat(conAgotado.get("productosSinStock").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(conAgotado.get("detalleStockBajo").findValuesAsText("id")).contains(String.valueOf(agotado));
        assertThat(conAgotado.get("detalleStockBajo").get(0).get("stock").asInt()).isZero();
    }

    @Test
    void elEmpleadoTambienVeElReporte() throws Exception {
        get("/api/reportes/inventario", tokenEmpleado).andExpect(status().isOk());
    }

    private JsonNode reporte() throws Exception {
        return cuerpo(get("/api/reportes/inventario", tokenAdmin).andExpect(status().isOk()));
    }

    private static void assertDiferencia(JsonNode antes, JsonNode despues, String campo, String esperada) {
        BigDecimal diferencia = dinero(despues.get(campo)).subtract(dinero(antes.get(campo)));
        assertThat(diferencia).as(campo).isEqualByComparingTo(esperada);
    }
}
