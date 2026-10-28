package com.utn.inventario.service;

import com.utn.inventario.dto.request.VentaRequest;
import com.utn.inventario.dto.response.VentaResponse;

import java.util.List;

public interface VentaService {
    List<VentaResponse> listar();
    VentaResponse obtenerPorId(Long id);
    VentaResponse crear(VentaRequest request, String emailUsuario);
    VentaResponse anular(Long id, String emailUsuario);
}
