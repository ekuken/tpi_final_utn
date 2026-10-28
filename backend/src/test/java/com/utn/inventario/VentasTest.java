package com.utn.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VentasTest extends ApiTestBase {

    @Test
    void ventaDescuentaElStockDeCadaProductoYCalculaElTotal() throws Exception {
        long agua = crearProducto("800", 10);
        long arroz = crearProducto("1200", 5);

        JsonNode v = cuerpo(post("/api/ventas", tokenEmpleado, Map.of("cliente", "  Juan  ",
                "items", List.of(item(agua, 3), item(arroz, 2))))
                .andExpect(status().isOk()));

        assertThat(v.get("estado").asText()).isEqualTo("CONFIRMADA");
        assertThat(v.get("cliente").asText()).isEqualTo("Juan");
        assertThat(dinero(v.get("total"))).isEqualByComparingTo("4800");
        assertThat(v.get("items")).hasSize(2);
        assertThat(stock(agua)).isEqualTo(7);
        assertThat(stock(arroz)).isEqualTo(3);
        assertThat(movimientos(agua).get(0).get("motivo").asText()).isEqualTo("Venta #" + v.get("id").asLong());
    }

    @Test
    void siUnProductoNoAlcanzaNoSeRegistraNada() throws Exception {
        long conStock = crearProducto("100", 10);
        long sinStock = crearProducto("100", 1);
        int ventasAntes = cuerpo(get("/api/ventas", tokenAdmin)).size();

        post("/api/ventas", tokenEmpleado, venta(List.of(item(conStock, 4), item(sinStock, 2))))
                .andExpect(status().isBadRequest());

        assertThat(stock(conStock)).isEqualTo(10);
        assertThat(stock(sinStock)).isEqualTo(1);
        assertThat(movimientos(conStock)).isEmpty();
        assertThat(cuerpo(get("/api/ventas", tokenAdmin)).size()).isEqualTo(ventasAntes);
    }

    @Test
    void elPrecioDeLaVentaNoCambiaSiDespuesCambiaElDelProducto() throws Exception {
        long id = crearProducto("100", 10);
        long venta = cuerpo(post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 1))))).get("id").asLong();

        JsonNode p = producto(id);
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("codigo", p.get("codigo").asText());
        cambios.put("nombre", p.get("nombre").asText());
        cambios.put("precio", 999);
        put("/api/productos/" + id, tokenAdmin, cambios).andExpect(status().isOk());

        get("/api/ventas/" + venta, tokenAdmin)
                .andExpect(jsonPath("$.total").value(100.0))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(100.0));
    }

    @Test
    void ventasInvalidasSeRechazan() throws Exception {
        long id = crearProducto("100", 10);
        post("/api/ventas", tokenEmpleado, venta(List.of()))
                .andExpect(status().isBadRequest());
        post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 1), item(id, 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("repetido")));
        post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 0))))
                .andExpect(status().isBadRequest());
        post("/api/ventas", tokenEmpleado, venta(List.of(item(999999, 1))))
                .andExpect(status().isNotFound());
        post("/api/ventas", tokenEmpleado, Map.of("cliente", "C".repeat(81), "items", List.of(item(id, 1))))
                .andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(10);
    }

    @Test
    void renglonNuloEnLaVentaDa400YNoErrorInterno() throws Exception {
        post("/api/ventas", tokenEmpleado, "{\"items\": [null]}").andExpect(status().isBadRequest());
    }

    @Test
    void noSePuedeVenderUnProductoDadoDeBaja() throws Exception {
        long id = crearProducto("100", 10);
        delete("/api/productos/" + id, tokenAdmin).andExpect(status().isNoContent());
        post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 1)))).andExpect(status().isBadRequest());
    }

    @Test
    void soloElAdminAnulaYElStockVuelve() throws Exception {
        long id = crearProducto("100", 10);
        long venta = cuerpo(post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 4))))).get("id").asLong();
        assertThat(stock(id)).isEqualTo(6);

        post("/api/ventas/" + venta + "/anular", tokenEmpleado, null).andExpect(status().isForbidden());
        assertThat(stock(id)).isEqualTo(6);

        post("/api/ventas/" + venta + "/anular", tokenAdmin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        assertThat(stock(id)).isEqualTo(10);
        assertThat(movimientos(id).get(0).get("motivo").asText()).isEqualTo("Anulación de venta #" + venta);

        post("/api/ventas/" + venta + "/anular", tokenAdmin, null).andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(10);
    }

    @Test
    void ventaInexistenteDa404() throws Exception {
        get("/api/ventas/999999", tokenAdmin).andExpect(status().isNotFound());
        post("/api/ventas/999999/anular", tokenAdmin, null).andExpect(status().isNotFound());
    }

    @Test
    void dosVentasSimultaneasDeLaUltimaUnidad() throws Exception {
        long id = crearProducto("100", 1);
        int intentos = 4;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < intentos; i++) {
                Callable<Integer> vender = () -> {
                    largada.await();
                    return post("/api/ventas", tokenEmpleado, venta(List.of(item(id, 1))))
                            .andReturn().getResponse().getStatus();
                };
                resultados.add(pool.submit(vender));
            }
            largada.countDown();

            int[] estados = new int[intentos];
            for (int i = 0; i < intentos; i++) {
                estados[i] = resultados.get(i).get();
            }

            long exitosas = Arrays.stream(estados).filter(s -> s == 200).count();
            assertThat(exitosas).as("ventas aceptadas, estados: %s", Arrays.toString(estados)).isEqualTo(1);
            assertThat(Arrays.stream(estados).allMatch(s -> s == 200 || s == 400 || s == 409))
                    .as("estados devueltos: %s", Arrays.toString(estados)).isTrue();
            assertThat(stock(id)).isZero();
            assertThat(movimientos(id)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
