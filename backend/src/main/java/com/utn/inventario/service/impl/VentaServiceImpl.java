package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.ItemVentaRequest;
import com.utn.inventario.dto.request.VentaRequest;
import com.utn.inventario.dto.response.ItemDetalleResponse;
import com.utn.inventario.dto.response.VentaResponse;
import com.utn.inventario.entity.EstadoVenta;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.entity.TipoMovimiento;
import com.utn.inventario.entity.Usuario;
import com.utn.inventario.entity.Venta;
import com.utn.inventario.entity.VentaItem;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.UsuarioRepository;
import com.utn.inventario.repository.VentaRepository;
import com.utn.inventario.service.MovimientoStockService;
import com.utn.inventario.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoStockService movimientoStockService;

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaRepository.findAllConDetalle().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public VentaResponse crear(VentaRequest request, String emailUsuario) {
        validarSinProductosRepetidos(request.getItems());
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);

        Venta venta = Venta.builder()
                .cliente(request.getCliente() != null && !request.getCliente().isBlank()
                        ? request.getCliente().trim() : null)
                .usuario(usuario)
                .build();

        for (ItemVentaRequest itemRequest : request.getItems()) {
            Producto producto = productoRepository.findById(itemRequest.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id " + itemRequest.getProductoId()));
            venta.agregarItem(VentaItem.builder()
                    .producto(producto)
                    .cantidad(itemRequest.getCantidad())
                    .precioUnitario(producto.getPrecio())
                    .build());
        }
        venta.setTotal(venta.getItems().stream()
                .map(VentaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        venta = ventaRepository.save(venta);
        String motivo = "Venta #" + venta.getId();
        for (VentaItem item : venta.getItems()) {
            movimientoStockService.aplicarMovimiento(
                    item.getProducto(), TipoMovimiento.SALIDA, item.getCantidad(), motivo, usuario);
        }

        return mapearAResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id, String emailUsuario) {
        Venta venta = buscarPorId(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new BusinessException("La venta #" + id + " ya está anulada");
        }

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);
        String motivo = "Anulación de venta #" + venta.getId();
        for (VentaItem item : venta.getItems()) {
            movimientoStockService.aplicarMovimiento(
                    item.getProducto(), TipoMovimiento.ENTRADA, item.getCantidad(), motivo, usuario);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return mapearAResponse(ventaRepository.save(venta));
    }

    private void validarSinProductosRepetidos(List<ItemVentaRequest> items) {
        Set<Long> vistos = new HashSet<>();
        for (ItemVentaRequest item : items) {
            if (!vistos.add(item.getProductoId())) {
                throw new BusinessException("Hay un producto repetido en la venta. Sumá las cantidades en un solo renglón.");
            }
        }
    }

    private Venta buscarPorId(Long id) {
        return ventaRepository.findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id " + id));
    }

    private VentaResponse mapearAResponse(Venta venta) {
        List<ItemDetalleResponse> items = venta.getItems().stream()
                .map(i -> ItemDetalleResponse.builder()
                        .productoId(i.getProducto().getId())
                        .productoCodigo(i.getProducto().getCodigo())
                        .productoNombre(i.getProducto().getNombre())
                        .cantidad(i.getCantidad())
                        .precioUnitario(i.getPrecioUnitario())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return VentaResponse.builder()
                .id(venta.getId())
                .cliente(venta.getCliente())
                .estado(venta.getEstado())
                .total(venta.getTotal())
                .usuarioNombre(venta.getUsuario() != null ? venta.getUsuario().getNombre() : "N/D")
                .fecha(venta.getFecha())
                .items(items)
                .build();
    }
}
