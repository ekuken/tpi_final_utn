package com.utn.inventario.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CLAVE = "clave-de-prueba-de-al-menos-treinta-y-dos-bytes-1234567890";
    private static final String OTRA_CLAVE = "otra-clave-distinta-tambien-de-mas-de-treinta-y-dos-bytes-xyz";

    private JwtService jwtService;
    private UserDetails ana;

    private static JwtService crearServicio(String clave, long expiracionMs) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "jwtSecret", clave);
        ReflectionTestUtils.setField(servicio, "jwtExpirationMs", expiracionMs);
        return servicio;
    }

    @BeforeEach
    void setUp() {
        jwtService = crearServicio(CLAVE, 60_000);
        ana = User.withUsername("ana@inventario.com").password("x").roles("EMPLEADO").build();
    }

    @Test
    void unTokenGeneradoEsValidoParaSuUsuario() {
        String token = jwtService.generarToken(ana);

        boolean valido = jwtService.esTokenValido(token, ana);

        assertThat(valido).isTrue();
    }

    @Test
    void extraerEmailDevuelveElEmailUsadoAlGenerarlo() {
        String token = jwtService.generarToken(ana);

        String email = jwtService.extraerEmail(token);

        assertThat(email).isEqualTo("ana@inventario.com");
    }

    @Test
    void unTokenNoEsValidoParaOtroUsuario() {
        String token = jwtService.generarToken(ana);
        UserDetails beto = User.withUsername("beto@inventario.com").password("x").roles("EMPLEADO").build();

        boolean valido = jwtService.esTokenValido(token, beto);

        assertThat(valido).isFalse();
    }

    @Test
    void unTokenManipuladoNoValida() {
        String[] partes = jwtService.generarToken(ana).split("\\.");
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@inventario.com\"}".getBytes());
        String manipulado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThatThrownBy(() -> jwtService.esTokenValido(manipulado, ana))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveNoValida() {
        String tokenAjeno = crearServicio(OTRA_CLAVE, 60_000).generarToken(ana);

        assertThatThrownBy(() -> jwtService.esTokenValido(tokenAjeno, ana))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenVencidoNoValida() {
        String vencido = crearServicio(CLAVE, -1_000).generarToken(ana);

        assertThatThrownBy(() -> jwtService.esTokenValido(vencido, ana))
                .isInstanceOf(JwtException.class);
    }
}
