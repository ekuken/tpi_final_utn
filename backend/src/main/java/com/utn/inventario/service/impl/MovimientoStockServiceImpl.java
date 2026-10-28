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
import com.utn.inventario.service.MovimientoStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoStockServiceImpl implements MovimientoStockService {

    private final MovimientoStockRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public MovimientoResponse registrarEntrada(MovimientoRequest request, String emailUsuario) {
        return registrarMovimiento(request, TipoMovimiento.ENTRADA, emailUsuario);
    }

    @Override
    @Transactional
    public MovimientoResponse registrarSalida(MovimientoRequest request, String emailUsuario) {
        return registrarMovimiento(request, TipoMovimiento.SALIDA, emailUsuario);
    }

    private MovimientoResponse registrarMovimiento(MovimientoRequest request, TipoMovimiento tipo, String emailUsuario) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + request.getProductoId()));

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);

        return mapearAResponse(aplicarMovimiento(producto, tipo, request.getCantidad(), request.getMotivo(), usuario));
    }

    @Override
    @Transactional
    public MovimientoStock aplicarMovimiento(Producto producto, TipoMovimiento tipo, int cantidad,
                                             String motivo, Usuario usuario) {
        if (!producto.isActivo()) {
            throw new BusinessException("No se pueden registrar movimientos sobre '" + producto.getNombre()
                    + "' porque está dado de baja");
        }

        int nuevoStock;
        if (tipo == TipoMovimiento.ENTRADA) {
            try {
                nuevoStock = Math.addExact(producto.getStock(), cantidad);
            } catch (ArithmeticException ex) {
                throw new BusinessException("La cantidad ingresada supera el stock máximo permitido para '"
                        + producto.getNombre() + "'");
            }
        } else {
            nuevoStock = producto.getStock() - cantidad;
            if (nuevoStock < 0) {
                throw new BusinessException(
                        "Stock insuficiente para '" + producto.getNombre() + "'. Stock actual: "
                                + producto.getStock() + ", cantidad solicitada: " + cantidad);
            }
        }

        producto.setStock(nuevoStock);
        productoRepository.save(producto);

        MovimientoStock movimiento = MovimientoStock.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockResultante(nuevoStock)
                .motivo(motivo)
                .usuario(usuario)
                .build();

        return movimientoRepository.save(movimiento);
    }

    @Override
    public List<MovimientoResponse> listarTodos() {
        return movimientoRepository.findAllByOrderByFechaDesc().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public List<MovimientoResponse> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    private MovimientoResponse mapearAResponse(MovimientoStock m) {
        return MovimientoResponse.builder()
                .id(m.getId())
                .productoId(m.getProducto().getId())
                .productoNombre(m.getProducto().getNombre())
                .productoCodigo(m.getProducto().getCodigo())
                .tipo(m.getTipo())
                .cantidad(m.getCantidad())
                .stockResultante(m.getStockResultante())
                .motivo(m.getMotivo())
                .usuarioNombre(m.getUsuario() != null ? m.getUsuario().getNombre() : "N/D")
                .fecha(m.getFecha())
                .build();
    }
}
