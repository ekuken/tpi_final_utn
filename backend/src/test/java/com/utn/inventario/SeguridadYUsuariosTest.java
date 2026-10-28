package com.utn.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadYUsuariosTest extends ApiTestBase {

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        post("/api/auth/login", null, Map.of("email", ADMIN_EMAIL, "password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void loginConContraseniaIncorrectaDa401() throws Exception {
        post("/api/auth/login", null, Map.of("email", ADMIN_EMAIL, "password", "otra"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConEmailInexistenteDa401() throws Exception {
        post("/api/auth/login", null, Map.of("email", "nadie@x.com", "password", "123456"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinDatosDa400() throws Exception {
        post("/api/auth/login", null, Map.of()).andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenNoSePuedeUsarLaApi() throws Exception {
        get("/api/productos", null).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInventadoDa401() throws Exception {
        get("/api/productos", "esto.no.esunjwt").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAlteradoDa401() throws Exception {
        String alterado = tokenAdmin.substring(0, tokenAdmin.length() - 4) + "AAAA";
        get("/api/productos", alterado).andExpect(status().isUnauthorized());
    }

    @Test
    void registroPublicoNuncaCreaAdministradores() throws Exception {
        String email = unico("intruso") + "@x.com";
        post("/api/auth/registro", null, Map.of("nombre", "Intruso", "email", email,
                "password", "123456", "rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("EMPLEADO"));
    }

    @Test
    void registroConEmailRepetidoDa400() throws Exception {
        post("/api/auth/registro", null, Map.of("nombre", "Otro", "email", ADMIN_EMAIL, "password", "123456"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroConElMismoEmailEnMayusculasTambienSeRechaza() throws Exception {
        post("/api/auth/registro", null, Map.of("nombre", "Clon", "email", "ADMIN@Inventario.com",
                "password", "123456"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elLoginNoDistingueMayusculasEnElEmail() throws Exception {
        post("/api/auth/login", null, Map.of("email", "  Admin@Inventario.COM ", "password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    @Test
    void elEmailSeGuardaEnMinusculas() throws Exception {
        String email = unico("Mixto") + "@X.com";
        post("/api/auth/registro", null, Map.of("nombre", "Mixto", "email", email, "password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()));
        login(email.toLowerCase(), "123456");
    }

    @Test
    void registroValidaContraseniaEmailYLargos() throws Exception {
        post("/api/auth/registro", null, Map.of("nombre", "A", "email", unico("a") + "@x.com", "password", "123"))
                .andExpect(status().isBadRequest());
        post("/api/auth/registro", null, Map.of("nombre", "A", "email", "no-es-email", "password", "123456"))
                .andExpect(status().isBadRequest());
        post("/api/auth/registro", null, Map.of("nombre", "N".repeat(81), "email", unico("a") + "@x.com",
                "password", "123456"))
                .andExpect(status().isBadRequest());
        post("/api/auth/registro", null, Map.of("nombre", "A", "email", unico("a") + "@x.com",
                "password", "p".repeat(73)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unEmpleadoNoPuedeGestionarUsuarios() throws Exception {
        get("/api/usuarios", tokenEmpleado).andExpect(status().isForbidden());
        post("/api/usuarios", tokenEmpleado, Map.of("nombre", "X", "email", unico("x") + "@x.com",
                "password", "123456", "rol", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminPuedeCrearOtroAdmin() throws Exception {
        post("/api/usuarios", tokenAdmin, Map.of("nombre", "Jefa", "email", unico("jefa") + "@x.com",
                "password", "123456", "rol", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void elAdminNoPuedeDesactivarseNiEliminarseASiMismo() throws Exception {
        long idAdmin = idUsuario(ADMIN_EMAIL);
        patch("/api/usuarios/" + idAdmin + "/estado", tokenAdmin, Map.of("activo", false))
                .andExpect(status().isBadRequest());
        delete("/api/usuarios/" + idAdmin, tokenAdmin).andExpect(status().isBadRequest());
    }

    @Test
    void alDesactivarUnUsuarioSuTokenDejaDeServirYNoPuedeEntrar() throws Exception {
        String email = unico("temporal") + "@x.com";
        JsonNode creado = cuerpo(post("/api/usuarios", tokenAdmin,
                Map.of("nombre", "Temporal", "email", email, "password", "123456"))
                .andExpect(status().isOk()));
        String suToken = login(email, "123456");
        get("/api/productos", suToken).andExpect(status().isOk());

        patch("/api/usuarios/" + creado.get("id").asLong() + "/estado", tokenAdmin, Map.of("activo", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        get("/api/productos", suToken).andExpect(status().isUnauthorized());
        post("/api/auth/login", null, Map.of("email", email, "password", "123456"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarEstadoSinIndicarActivoNoDesactivaAlUsuario() throws Exception {
        long id = cuerpo(post("/api/usuarios", tokenAdmin, Map.of("nombre", "Sin estado",
                "email", unico("estado") + "@x.com", "password", "123456"))).get("id").asLong();

        patch("/api/usuarios/" + id + "/estado", tokenAdmin, Map.of()).andExpect(status().isBadRequest());

        get("/api/usuarios/" + id, tokenAdmin).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void unUsuarioConVentasNoSeEliminaPeroUnoSinRegistrosSi() throws Exception {
        String email = unico("vendedor") + "@x.com";
        long id = cuerpo(post("/api/usuarios", tokenAdmin,
                Map.of("nombre", "Vendedor", "email", email, "password", "123456"))).get("id").asLong();
        String suToken = login(email, "123456");
        long producto = crearProducto("100", 5);
        post("/api/ventas", suToken, venta(java.util.List.of(item(producto, 1)))).andExpect(status().isOk());

        delete("/api/usuarios/" + id, tokenAdmin).andExpect(status().isBadRequest());

        long idSinRegistros = cuerpo(post("/api/usuarios", tokenAdmin, Map.of("nombre", "Nuevo",
                "email", unico("nuevo") + "@x.com", "password", "123456"))).get("id").asLong();
        delete("/api/usuarios/" + idSinRegistros, tokenAdmin).andExpect(status().isNoContent());
        get("/api/usuarios/" + idSinRegistros, tokenAdmin).andExpect(status().isNotFound());
    }

    private long idUsuario(String email) throws Exception {
        for (JsonNode u : cuerpo(get("/api/usuarios", tokenAdmin).andExpect(status().isOk()))) {
            if (u.get("email").asText().equals(email)) {
                return u.get("id").asLong();
            }
        }
        throw new AssertionError("No existe el usuario " + email);
    }

    @Test
    void laDocumentacionSwaggerEsPublica() throws Exception {
        get("/v3/api-docs", null).andExpect(status().isOk());
    }
}
