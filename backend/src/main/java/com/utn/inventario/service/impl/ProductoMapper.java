package com.utn.inventario.service.impl;

import com.utn.inventario.dto.response.ProductoResponse;
import com.utn.inventario.entity.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ProductoMapper {

    private ProductoMapper() {
    }

    static ProductoResponse toResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .precioCosto(producto.getPrecioCosto())
                .margen(calcularMargen(producto.getPrecio(), producto.getPrecioCosto()))
                .stock(producto.getStock())
                .stockMinimo(producto.getStockMinimo())
                .stockBajo(producto.getStock() <= producto.getStockMinimo())
                .categoriaId(producto.getCategoria() != null ? producto.getCategoria().getId() : null)
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null)
                .proveedorId(producto.getProveedor() != null ? producto.getProveedor().getId() : null)
                .proveedorNombre(producto.getProveedor() != null ? producto.getProveedor().getNombre() : null)
                .activo(producto.isActivo())
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }

    static BigDecimal calcularMargen(BigDecimal precioVenta, BigDecimal precioCosto) {
        if (precioVenta == null || precioCosto == null || precioCosto.signum() == 0) {
            return null;
        }
        return precioVenta.subtract(precioCosto)
                .multiply(BigDecimal.valueOf(100))
                .divide(precioCosto, 1, RoundingMode.HALF_UP);
    }
}
