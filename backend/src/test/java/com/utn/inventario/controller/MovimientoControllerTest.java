package com.utn.inventario.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.utn.inventario.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovimientoControllerTest extends ApiTestBase {

    @Test
    void flujoCompletoDeStockConSalidaExcesivaRechazada() throws Exception {
        long id = crearProducto("500", 10);

        ResultActions entrada = post("/api/movimientos/entrada", tokenEmpleado,
                Map.of("productoId", id, "cantidad", 5, "motivo", "compra"));

        entrada.andExpect(status().isOk()).andExpect(jsonPath("$.stockResultante").value(15));
        assertThat(stock(id)).isEqualTo(15);
        JsonNode historial = movimientos(id);
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).get("tipo").asText()).isEqualTo("ENTRADA");
        assertThat(historial.get(0).get("usuarioNombre").asText()).isEqualTo("Empleado Demo");

        ResultActions salida = post("/api/movimientos/salida", tokenEmpleado,
                Map.of("productoId", id, "cantidad", 16));

        salida.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
        assertThat(stock(id)).isEqualTo(15);
        assertThat(movimientos(id)).hasSize(1);
    }

    @ParameterizedTest(name = "cantidad {0}")
    @ValueSource(ints = {0, -1, -50})
    void salidaConCantidadCeroONegativaDevuelve400(int cantidad) throws Exception {
        long id = crearProducto("500", 10);

        ResultActions respuesta = post("/api/movimientos/salida", tokenEmpleado,
                Map.of("productoId", id, "cantidad", cantidad));

        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("cantidad:")));
        assertThat(stock(id)).isEqualTo(10);
    }
}
