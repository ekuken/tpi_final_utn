package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.RegistroUsuarioRequest;
import com.utn.inventario.dto.response.AuthResponse;
import com.utn.inventario.entity.RolUsuario;
import com.utn.inventario.entity.Usuario;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.repository.UsuarioRepository;
import com.utn.inventario.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl servicio;

    private static RegistroUsuarioRequest pedidoQueIntentaSerAdmin() {
        RegistroUsuarioRequest request = new RegistroUsuarioRequest();
        request.setNombre("Intruso");
        request.setEmail("intruso@x.com");
        request.setPassword("123456");
        request.setRol(RolUsuario.ADMIN);
        return request;
    }

    @Test
    void registrarFuerzaElRolEmpleadoAunqueElPedidoSoliciteAdmin() {
        when(usuarioRepository.existsByEmailIgnoreCase("intruso@x.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(userDetailsService.loadUserByUsername("intruso@x.com"))
                .thenReturn(User.withUsername("intruso@x.com").password("hash").roles("EMPLEADO").build());
        when(jwtService.generarToken(any())).thenReturn("token");
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        AuthResponse respuesta = servicio.registrar(pedidoQueIntentaSerAdmin());

        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(RolUsuario.EMPLEADO);
        assertThat(respuesta.getRol()).isEqualTo("EMPLEADO");
    }

    @Test
    void registrarGuardaLaContraseniaHasheadaYNoEnTextoPlano() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(User.withUsername("intruso@x.com").password("hash-bcrypt").roles("EMPLEADO").build());
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        servicio.registrar(pedidoQueIntentaSerAdmin());

        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getPassword()).isEqualTo("hash-bcrypt");
    }

    @Test
    void registrarConEmailDuplicadoLanzaExcepcionYNoGuarda() {
        when(usuarioRepository.existsByEmailIgnoreCase("intruso@x.com")).thenReturn(true);
        RegistroUsuarioRequest pedido = pedidoQueIntentaSerAdmin();

        assertThatThrownBy(() -> servicio.registrar(pedido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
        verify(usuarioRepository, never()).save(any());
    }
}
