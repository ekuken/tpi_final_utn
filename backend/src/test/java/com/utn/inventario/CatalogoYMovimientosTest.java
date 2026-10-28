package com.utn.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoYMovimientosTest extends ApiTestBase {

    @Test
    void categoriaConNombreRepetidoSeRechazaAunqueCambienLasMayusculas() throws Exception {
        String nombre = unico("Lacteos");
        post("/api/categorias", tokenAdmin, Map.of("nombre", nombre)).andExpect(status().isOk());
        post("/api/categorias", tokenAdmin, Map.of("nombre", nombre.toUpperCase()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoriaConProductosNoSeEliminaYSinProductosSi() throws Exception {
        long conProductos = crearCategoria();
        crearProducto(null, conProductos, "10", "5", 1, 0);
        delete("/api/categorias/" + conProductos, tokenAdmin).andExpect(status().isBadRequest());

        long vacia = crearCategoria();
        delete("/api/categorias/" + vacia, tokenAdmin).andExpect(status().isNoContent());
        get("/api/categorias/" + vacia, tokenAdmin).andExpect(status().isNotFound());
    }

    @Test
    void proveedorConProductosNoSeElimina() throws Exception {
        long proveedor = crearProveedor();
        crearProducto(proveedor, null, "10", "5", 1, 0);
        delete("/api/proveedores/" + proveedor, tokenAdmin).andExpect(status().isBadRequest());
    }

    @Test
    void proveedorValidaLargoDeLosCampos() throws Exception {
        post("/api/proveedores", tokenAdmin, Map.of("nombre", "P", "telefono", "1".repeat(21)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearProductoCalculaMargenYStockBajo() throws Exception {
        long id = crearProducto(null, null, "150", "100", 3, 5);
        JsonNode p = producto(id);
        assertThat(dinero(p.get("margen"))).isEqualByComparingTo("50.0");
        assertThat(p.get("stockBajo").asBoolean()).isTrue();

        JsonNode stockBajo = cuerpo(get("/api/productos/stock-bajo", tokenEmpleado).andExpect(status().isOk()));
        assertThat(stockBajo.findValuesAsText("id")).contains(String.valueOf(id));
    }

    @Test
    void codigoDeProductoRepetidoSeRechaza() throws Exception {
        String codigo = producto(crearProducto("10", 0)).get("codigo").asText();
        post("/api/productos", tokenAdmin, Map.of("codigo", codigo, "nombre", "Otro", "precio", 5))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codigoRepetidoConOtrasMayusculasTambienSeRechaza() throws Exception {
        String codigo = unico("ab-");
        post("/api/productos", tokenAdmin, Map.of("codigo", codigo, "nombre", "X", "precio", 5))
                .andExpect(status().isOk());
        post("/api/productos", tokenAdmin, Map.of("codigo", codigo.toUpperCase(), "nombre", "Y", "precio", 5))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockInicialEnormeSeRechaza() throws Exception {
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", 10,
                "stockInicial", 2_000_000_000))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productoValidaPrecioYLargos() throws Exception {
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", -1))
                .andExpect(status().isBadRequest());
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", 10,
                "precioCosto", -1))
                .andExpect(status().isBadRequest());
        post("/api/productos", tokenAdmin, Map.of("codigo", "C".repeat(21), "nombre", "X", "precio", 10))
                .andExpect(status().isBadRequest());
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", 10.999))
                .andExpect(status().isBadRequest());
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", 10,
                "stockInicial", -3))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarUnProductoNoModificaSuStock() throws Exception {
        long id = crearProducto("10", 7);
        JsonNode p = producto(id);
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("codigo", p.get("codigo").asText());
        cambios.put("nombre", "Nombre nuevo");
        cambios.put("precio", 20);
        cambios.put("precioCosto", 12);
        cambios.put("stockInicial", 999);

        put("/api/productos/" + id, tokenAdmin, cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"))
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.precioCosto").value(12));
    }

    @Test
    void bajaLogicaSacaAlProductoDelListadoPeroConservaElHistorial() throws Exception {
        long id = crearProducto("10", 5);
        post("/api/movimientos/entrada", tokenAdmin, Map.of("productoId", id, "cantidad", 1))
                .andExpect(status().isOk());

        delete("/api/productos/" + id, tokenAdmin).andExpect(status().isNoContent());

        JsonNode listado = cuerpo(get("/api/productos", tokenAdmin));
        assertThat(listado.findValuesAsText("id")).doesNotContain(String.valueOf(id));
        assertThat(producto(id).get("activo").asBoolean()).isFalse();
        assertThat(movimientos(id)).hasSize(1);
        post("/api/movimientos/entrada", tokenAdmin, Map.of("productoId", id, "cantidad", 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarProductoPorNombre() throws Exception {
        String nombre = unico("Mermelada");
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("M"), "nombre", nombre, "precio", 10))
                .andExpect(status().isOk());
        JsonNode r = cuerpo(get("/api/productos?nombre=" + nombre.toLowerCase(), tokenEmpleado));
        assertThat(r).hasSize(1);
    }

    @Test
    void productoInexistenteDa404() throws Exception {
        get("/api/productos/999999", tokenAdmin).andExpect(status().isNotFound());
        post("/api/productos", tokenAdmin, Map.of("codigo", unico("V"), "nombre", "X", "precio", 1,
                "categoriaId", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void entradaYSalidaActualizanElStockYQuedanEnElHistorial() throws Exception {
        long id = crearProducto("10", 10);
        post("/api/movimientos/entrada", tokenEmpleado, Map.of("productoId", id, "cantidad", 5, "motivo", "compra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockResultante").value(15))
                .andExpect(jsonPath("$.usuarioNombre").value("Empleado Demo"));
        post("/api/movimientos/salida", tokenEmpleado, Map.of("productoId", id, "cantidad", 15))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockResultante").value(0));

        JsonNode historial = movimientos(id);
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).get("tipo").asText()).isEqualTo("SALIDA");
    }

    @Test
    void salidaMayorAlStockSeRechazaYElStockNoCambia() throws Exception {
        long id = crearProducto("10", 3);
        post("/api/movimientos/salida", tokenEmpleado, Map.of("productoId", id, "cantidad", 4))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
        assertThat(stock(id)).isEqualTo(3);
        assertThat(movimientos(id)).isEmpty();
    }

    @Test
    void cantidadCeroONegativaSeRechaza() throws Exception {
        long id = crearProducto("10", 3);
        post("/api/movimientos/entrada", tokenEmpleado, Map.of("productoId", id, "cantidad", 0))
                .andExpect(status().isBadRequest());
        post("/api/movimientos/salida", tokenEmpleado, Map.of("productoId", id, "cantidad", -5))
                .andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(3);
    }

    @Test
    void entradasEnormesNoDejanElStockNegativo() throws Exception {
        long id = crearProducto("10", 0);
        post("/api/movimientos/entrada", tokenEmpleado, Map.of("productoId", id, "cantidad", 2_000_000_000))
                .andExpect(status().isBadRequest());
        post("/api/movimientos/entrada", tokenEmpleado, Map.of("productoId", id, "cantidad", 2_000_000_000))
                .andExpect(status().isBadRequest());
        assertThat(stock(id)).isZero();
    }

    @Test
    void jsonMalFormadoDa400YNoErrorInterno() throws Exception {
        post("/api/productos", tokenAdmin, "{ esto no es json").andExpect(status().isBadRequest());
    }

    @Test
    void tipoDeDatoIncorrectoDa400() throws Exception {
        post("/api/movimientos/entrada", tokenAdmin, "{\"productoId\": 1, \"cantidad\": \"muchos\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void idNoNumericoEnLaUrlDa400() throws Exception {
        get("/api/productos/abc", tokenAdmin).andExpect(status().isBadRequest());
    }

    @Test
    void rutaInexistenteDa404() throws Exception {
        get("/api/no-existe", tokenAdmin).andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitidoDa405() throws Exception {
        delete("/api/reportes/inventario", tokenAdmin).andExpect(status().isMethodNotAllowed());
    }
}
