package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.ProductoRequest;
import com.utn.inventario.dto.response.ProductoResponse;
import com.utn.inventario.entity.Categoria;
import com.utn.inventario.entity.Producto;
import com.utn.inventario.entity.Proveedor;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.CategoriaRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.ProveedorRepository;
import com.utn.inventario.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    @Override
    public List<ProductoResponse> listar() {
        return productoRepository.findAllActivos().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public List<ProductoResponse> listarConStockBajo() {
        return productoRepository.findConStockBajo().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public List<ProductoResponse> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public ProductoResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByCodigoIgnoreCase(request.getCodigo())) {
            throw new BusinessException("Ya existe un producto con el código '" + request.getCodigo() + "'");
        }

        Producto producto = Producto.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .precioCosto(request.getPrecioCosto() != null ? request.getPrecioCosto() : BigDecimal.ZERO)
                .stock(request.getStockInicial() != null ? request.getStockInicial() : 0)
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0)
                .categoria(resolverCategoria(request.getCategoriaId()))
                .proveedor(resolverProveedor(request.getProveedorId()))
                .activo(true)
                .build();

        return mapearAResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarPorId(id);

        if (!producto.getCodigo().equalsIgnoreCase(request.getCodigo())
                && productoRepository.existsByCodigoIgnoreCase(request.getCodigo())) {
            throw new BusinessException("Ya existe un producto con el código '" + request.getCodigo() + "'");
        }

        producto.setCodigo(request.getCodigo());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        if (request.getPrecioCosto() != null) {
            producto.setPrecioCosto(request.getPrecioCosto());
        }
        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }
        producto.setCategoria(resolverCategoria(request.getCategoriaId()));
        producto.setProveedor(resolverProveedor(request.getProveedorId()));

        return mapearAResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) return null;
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + categoriaId));
    }

    private Proveedor resolverProveedor(Long proveedorId) {
        if (proveedorId == null) return null;
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id " + proveedorId));
    }

    private Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }

    private ProductoResponse mapearAResponse(Producto producto) {
        return ProductoMapper.toResponse(producto);
    }
}
