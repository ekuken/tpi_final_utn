package com.utn.inventario.service.impl;

import com.utn.inventario.dto.response.ReporteInventarioResponse;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.repository.OrdenCompraRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @InjectMocks
    private ReporteServiceImpl servicio;

    private final Producto caramelo = producto("0.10", "0.05", 3, 1);
    private final Producto chicle = producto("0.20", "0.10", 3, 5);
    private final Producto gaseosa = producto("1500.55", "1000.00", 2, 2);

    private static Producto producto(String precio, String costo, int stock, int minimo) {
        return Producto.builder().codigo("C").nombre("N").precio(new BigDecimal(precio))
                .precioCosto(new BigDecimal(costo)).stock(stock).stockMinimo(minimo).activo(true).build();
    }

    @BeforeEach
    void setUp() {
        when(productoRepository.findAllActivos()).thenReturn(List.of(caramelo, chicle, gaseosa));
        when(productoRepository.findConStockBajo()).thenReturn(List.of(chicle, gaseosa));
        lenient().when(ventaRepository.sumarTotalDesde(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void cuentaLosProductosActivos() {
        ReporteInventarioResponse reporte = servicio.generarReporteInventario();

        assertThat(reporte.getTotalProductos()).isEqualTo(3);
    }

    @Test
    void sumaLasUnidadesEnStock() {
        ReporteInventarioResponse reporte = servicio.generarReporteInventario();

        assertThat(reporte.getTotalUnidadesEnStock()).isEqualTo(8);
    }

    @Test
    void cuentaLosProductosConStockBajo() {
        ReporteInventarioResponse reporte = servicio.generarReporteInventario();

        assertThat(reporte.getProductosConStockBajo()).isEqualTo(2);
        assertThat(reporte.getDetalleStockBajo()).hasSize(2);
    }

    @Test
    void laValorizacionUsaBigDecimalYEsExacta() {
        ReporteInventarioResponse reporte = servicio.generarReporteInventario();

        assertThat(reporte.getValorTotalInventario().compareTo(new BigDecimal("3002.00"))).isZero();
    }

    @Test
    void laValorizacionACostoYLaGananciaSonExactas() {
        ReporteInventarioResponse reporte = servicio.generarReporteInventario();

        assertThat(reporte.getValorInventarioCosto().compareTo(new BigDecimal("2000.45"))).isZero();
        assertThat(reporte.getGananciaPotencial().compareTo(new BigDecimal("1001.55"))).isZero();
    }
}
