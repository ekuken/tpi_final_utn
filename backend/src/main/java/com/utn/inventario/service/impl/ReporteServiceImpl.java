package com.utn.inventario.service.impl;

import com.utn.inventario.dto.response.ProductoResponse;
import com.utn.inventario.dto.response.ReporteInventarioResponse;
import com.utn.inventario.entity.EstadoOrdenCompra;
import com.utn.inventario.entity.EstadoVenta;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.repository.OrdenCompraRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.VentaRepository;
import com.utn.inventario.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    @Override
    @Transactional(readOnly = true)
    public ReporteInventarioResponse generarReporteInventario() {
        List<Producto> activos = productoRepository.findAllActivos();

        long totalUnidades = activos.stream().mapToLong(Producto::getStock).sum();
        BigDecimal valorVenta = valorizar(activos, Producto::getPrecio);
        BigDecimal valorCosto = valorizar(activos, Producto::getPrecioCosto);

        List<ProductoResponse> detalleStockBajo = productoRepository.findConStockBajo().stream()
                .sorted(Comparator.comparing(Producto::getStock))
                .map(ProductoMapper::toResponse)
                .toList();
        long sinStock = detalleStockBajo.stream().filter(p -> p.getStock() == 0).count();

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return ReporteInventarioResponse.builder()
                .totalProductos(activos.size())
                .totalUnidadesEnStock(totalUnidades)
                .valorTotalInventario(valorVenta)
                .valorInventarioCosto(valorCosto)
                .gananciaPotencial(valorVenta.subtract(valorCosto))
                .productosConStockBajo(detalleStockBajo.size())
                .productosSinStock(sinStock)
                .detalleStockBajo(detalleStockBajo)
                .ventasDelMes(ventaRepository.sumarTotalDesde(EstadoVenta.CONFIRMADA, inicioMes))
                .cantidadVentasDelMes(ventaRepository.countByEstadoAndFechaGreaterThanEqual(EstadoVenta.CONFIRMADA, inicioMes))
                .ordenesCompraPendientes(ordenCompraRepository.countByEstado(EstadoOrdenCompra.PENDIENTE))
                .build();
    }

    private BigDecimal valorizar(List<Producto> productos, Function<Producto, BigDecimal> precio) {
        return productos.stream()
                .map(p -> precio.apply(p).multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
