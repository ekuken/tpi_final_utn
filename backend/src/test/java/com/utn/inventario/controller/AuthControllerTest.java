package com.utn.inventario.controller;

import com.utn.inventario.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ApiTestBase {

    @Test
    void loginConCredencialesValidasDevuelve200YUnToken() throws Exception {
        Map<String, String> credenciales = Map.of("email", ADMIN_EMAIL, "password", "admin123");

        ResultActions respuesta = post("/api/auth/login", null, credenciales);

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void loginConContraseniaIncorrectaDevuelve401() throws Exception {
        Map<String, String> credenciales = Map.of("email", ADMIN_EMAIL, "password", "incorrecta");

        ResultActions respuesta = post("/api/auth/login", null, credenciales);

        respuesta.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
