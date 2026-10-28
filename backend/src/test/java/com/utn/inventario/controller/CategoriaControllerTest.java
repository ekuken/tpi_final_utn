package com.utn.inventario.controller;

import com.utn.inventario.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoriaControllerTest extends ApiTestBase {

    @Test
    void crearCategoriaDuplicadaEnMayusculasDevuelve400ContraLaBaseReal() throws Exception {
        Map<String, String> duplicada = Map.of("nombre", "BEBIDAS");

        ResultActions respuesta = post("/api/categorias", tokenAdmin, duplicada);

        respuesta.andExpect(status().isBadRequest());
    }
}
