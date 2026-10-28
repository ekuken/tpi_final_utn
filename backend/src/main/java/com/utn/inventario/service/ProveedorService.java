package com.utn.inventario.service;

import com.utn.inventario.dto.request.ProveedorRequest;
import com.utn.inventario.dto.response.ProveedorResponse;

import java.util.List;

public interface ProveedorService {
    List<ProveedorResponse> listar();
    ProveedorResponse obtenerPorId(Long id);
    ProveedorResponse crear(ProveedorRequest request);
    ProveedorResponse actualizar(Long id, ProveedorRequest request);
    void eliminar(Long id);
}
