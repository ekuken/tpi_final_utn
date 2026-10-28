package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.MovimientoRequest;
import com.utn.inventario.dto.response.MovimientoResponse;
import com.utn.inventario.entity.MovimientoStock;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.entity.TipoMovimiento;
import com.utn.inventario.entity.Usuario;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.MovimientoStockRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceImplTest {

    private static final String EMAIL = "empleado@inventario.com";

    @Mock
    private MovimientoStockRepository movimientoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MovimientoStockServiceImpl servicio;

    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = Producto.builder().id(1L).codigo("YER-001").nombre("Yerba 1kg")
                .precio(new BigDecimal("3500")).stock(10).stockMinimo(2).activo(true).build();
        usuario = Usuario.builder().id(7L).nombre("Empleado Demo").email(EMAIL).build();
        lenient().when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static MovimientoRequest request(long productoId, int cantidad) {
        MovimientoRequest request = new MovimientoRequest();
        request.setProductoId(productoId);
        request.setCantidad(cantidad);
        request.setMotivo("prueba");
        return request;
    }

    private void productoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
    }

    private void usuarioExiste() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    @Test
    void registrarEntradaSumaAlStockDelProducto() {
        productoExiste();
        usuarioExiste();

        servicio.registrarEntrada(request(1L, 5), EMAIL);

        assertThat(producto.getStock()).isEqualTo(15);
        verify(productoRepository).save(producto);
    }

    @Test
    void registrarEntradaGuardaElMovimientoConElStockResultanteCorrecto() {
        productoExiste();
        usuarioExiste();
        ArgumentCaptor<MovimientoStock> guardado = ArgumentCaptor.forClass(MovimientoStock.class);

        MovimientoResponse respuesta = servicio.registrarEntrada(request(1L, 5), EMAIL);

        verify(movimientoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(guardado.getValue().getCantidad()).isEqualTo(5);
        assertThat(guardado.getValue().getStockResultante()).isEqualTo(15);
        assertThat(respuesta.getStockResultante()).isEqualTo(15);
        assertThat(respuesta.getUsuarioNombre()).isEqualTo("Empleado Demo");
    }

    @Test
    void registrarSalidaRestaDelStockDelProducto() {
        productoExiste();
        usuarioExiste();

        MovimientoResponse respuesta = servicio.registrarSalida(request(1L, 4), EMAIL);

        assertThat(producto.getStock()).isEqualTo(6);
        assertThat(respuesta.getTipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(respuesta.getStockResultante()).isEqualTo(6);
    }

    @Test
    void salidaDeTodoElStockDisponibleDejaElStockEnCero() {
        productoExiste();
        usuarioExiste();

        servicio.registrarSalida(request(1L, 10), EMAIL);

        assertThat(producto.getStock()).isZero();
    }

    @Test
    void salidaMayorAlStockDisponibleLanzaExcepcionConStockActualYCantidadPedida() {
        productoExiste();
        MovimientoRequest pedido = request(1L, 11);

        assertThatThrownBy(() -> servicio.registrarSalida(pedido, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock actual: 10")
                .hasMessageContaining("cantidad solicitada: 11");
    }

    @Test
    void salidaMayorAlStockDisponibleLanzaExcepcionYNoModificaElStock() {
        productoExiste();
        MovimientoRequest pedido = request(1L, 11);

        catchThrowable(() -> servicio.registrarSalida(pedido, EMAIL));

        assertThat(producto.getStock()).isEqualTo(10);
        verify(productoRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void movimientoSobreProductoDadoDeBajaLanzaExcepcion() {
        producto.setActivo(false);
        productoExiste();
        MovimientoRequest pedido = request(1L, 1);

        assertThatThrownBy(() -> servicio.registrarEntrada(pedido, EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dado de baja");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void productoInexistenteLanzaResourceNotFoundException() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        MovimientoRequest pedido = request(99L, 1);

        assertThatThrownBy(() -> servicio.registrarEntrada(pedido, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void siElUsuarioNoExisteElMovimientoSeGuardaIgualConUsuarioND() {
        productoExiste();
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        MovimientoResponse respuesta = servicio.registrarEntrada(request(1L, 3), "fantasma@x.com");

        verify(movimientoRepository).save(any(MovimientoStock.class));
        assertThat(respuesta.getUsuarioNombre()).isEqualTo("N/D");
        assertThat(producto.getStock()).isEqualTo(13);
    }

    @Test
    void unaEntradaEnormeNoDesbordaElStock() {
        producto.setStock(Integer.MAX_VALUE - 5);

        assertThatThrownBy(() -> servicio.aplicarMovimiento(producto, TipoMovimiento.ENTRADA, 10, null, null))
                .isInstanceOf(BusinessException.class);
        assertThat(producto.getStock()).isEqualTo(Integer.MAX_VALUE - 5);
    }

    private static void catchThrowable(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException esperada) {
        }
    }
}
