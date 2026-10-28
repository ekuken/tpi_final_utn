package com.utn.inventario.service;

import com.utn.inventario.dto.request.ProductoRequest;
import com.utn.inventario.dto.response.ProductoResponse;

import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listar();
    List<ProductoResponse> listarConStockBajo();
    List<ProductoResponse> buscarPorNombre(String nombre);
    ProductoResponse obtenerPorId(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
}
