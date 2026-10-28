package com.utn.inventario.controller;

import com.utn.inventario.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorizacionTest extends ApiTestBase {

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        ResultActions respuesta = get("/api/productos", null);

        respuesta.andExpect(status().isUnauthorized());
    }

    @Test
    void elError401VieneEnJsonConLaEstructuraDeErrorResponse() throws Exception {
        ResultActions respuesta = get("/api/productos", null);

        respuesta.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/productos"));
    }

    @Test
    void unEmpleadoPidiendoLaListaDeUsuariosRecibe403() throws Exception {
        ResultActions respuesta = get("/api/usuarios", tokenEmpleado);

        respuesta.andExpect(status().isForbidden());
    }

    @Test
    void elError403VieneEnJsonConLaEstructuraDeErrorResponse() throws Exception {
        ResultActions respuesta = get("/api/usuarios", tokenEmpleado);

        respuesta.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/usuarios"));
    }

    @Test
    void unAdminPidiendoLaListaDeUsuariosRecibe200() throws Exception {
        ResultActions respuesta = get("/api/usuarios", tokenAdmin);

        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
