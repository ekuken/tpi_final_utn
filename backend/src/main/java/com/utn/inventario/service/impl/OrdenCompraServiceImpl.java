package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.ItemOrdenCompraRequest;
import com.utn.inventario.dto.request.OrdenCompraRequest;
import com.utn.inventario.dto.response.ItemDetalleResponse;
import com.utn.inventario.dto.response.OrdenCompraResponse;
import com.utn.inventario.entity.EstadoOrdenCompra;
import com.utn.inventario.entity.OrdenCompra;
import com.utn.inventario.entity.OrdenCompraItem;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.entity.Proveedor;
import com.utn.inventario.entity.TipoMovimiento;
import com.utn.inventario.entity.Usuario;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.OrdenCompraRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.ProveedorRepository;
import com.utn.inventario.repository.UsuarioRepository;
import com.utn.inventario.service.MovimientoStockService;
import com.utn.inventario.service.OrdenCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoStockService movimientoStockService;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompraResponse> listar() {
        return ordenCompraRepository.findAllConDetalle().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompraResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest request, String emailUsuario) {
        validarSinProductosRepetidos(request.getItems());

        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado con id " + request.getProveedorId()));

        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .observaciones(request.getObservaciones() != null && !request.getObservaciones().isBlank()
                        ? request.getObservaciones().trim() : null)
                .usuario(usuarioRepository.findByEmail(emailUsuario).orElse(null))
                .build();

        for (ItemOrdenCompraRequest itemRequest : request.getItems()) {
            Producto producto = productoRepository.findById(itemRequest.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id " + itemRequest.getProductoId()));
            if (!producto.isActivo()) {
                throw new BusinessException("'" + producto.getNombre() + "' está dado de baja y no se puede pedir");
            }
            orden.agregarItem(OrdenCompraItem.builder()
                    .producto(producto)
                    .cantidad(itemRequest.getCantidad())
                    .costoUnitario(itemRequest.getCostoUnitario())
                    .build());
        }
        orden.setTotal(orden.getItems().stream()
                .map(OrdenCompraItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return mapearAResponse(ordenCompraRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenCompraResponse recibir(Long id, String emailUsuario) {
        OrdenCompra orden = buscarPorId(id);
        validarPendiente(orden);

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);
        String motivo = "Orden de compra #" + orden.getId() + " - " + orden.getProveedor().getNombre();
        for (OrdenCompraItem item : orden.getItems()) {
            item.getProducto().setPrecioCosto(item.getCostoUnitario());
            movimientoStockService.aplicarMovimiento(
                    item.getProducto(), TipoMovimiento.ENTRADA, item.getCantidad(), motivo, usuario);
        }

        orden.setEstado(EstadoOrdenCompra.RECIBIDA);
        orden.setFechaRecepcion(LocalDateTime.now());
        return mapearAResponse(ordenCompraRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenCompraResponse cancelar(Long id) {
        OrdenCompra orden = buscarPorId(id);
        validarPendiente(orden);
        orden.setEstado(EstadoOrdenCompra.CANCELADA);
        return mapearAResponse(ordenCompraRepository.save(orden));
    }

    private void validarPendiente(OrdenCompra orden) {
        if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE) {
            throw new BusinessException("La orden #" + orden.getId() + " ya está "
                    + orden.getEstado().name().toLowerCase() + " y no se puede modificar");
        }
    }

    private void validarSinProductosRepetidos(List<ItemOrdenCompraRequest> items) {
        Set<Long> vistos = new HashSet<>();
        for (ItemOrdenCompraRequest item : items) {
            if (!vistos.add(item.getProductoId())) {
                throw new BusinessException("Hay un producto repetido en la orden. Sumá las cantidades en un solo renglón.");
            }
        }
    }

    private OrdenCompra buscarPorId(Long id) {
        return ordenCompraRepository.findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con id " + id));
    }

    private OrdenCompraResponse mapearAResponse(OrdenCompra orden) {
        List<ItemDetalleResponse> items = orden.getItems().stream()
                .map(i -> ItemDetalleResponse.builder()
                        .productoId(i.getProducto().getId())
                        .productoCodigo(i.getProducto().getCodigo())
                        .productoNombre(i.getProducto().getNombre())
                        .cantidad(i.getCantidad())
                        .precioUnitario(i.getCostoUnitario())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return OrdenCompraResponse.builder()
                .id(orden.getId())
                .proveedorId(orden.getProveedor().getId())
                .proveedorNombre(orden.getProveedor().getNombre())
                .estado(orden.getEstado())
                .observaciones(orden.getObservaciones())
                .total(orden.getTotal())
                .usuarioNombre(orden.getUsuario() != null ? orden.getUsuario().getNombre() : "N/D")
                .fechaCreacion(orden.getFechaCreacion())
                .fechaRecepcion(orden.getFechaRecepcion())
                .items(items)
                .build();
    }
}
