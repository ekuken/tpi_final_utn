package com.utn.inventario.service;

import com.utn.inventario.dto.request.OrdenCompraRequest;
import com.utn.inventario.dto.response.OrdenCompraResponse;

import java.util.List;

public interface OrdenCompraService {
    List<OrdenCompraResponse> listar();
    OrdenCompraResponse obtenerPorId(Long id);
    OrdenCompraResponse crear(OrdenCompraRequest request, String emailUsuario);
    OrdenCompraResponse recibir(Long id, String emailUsuario);
    OrdenCompraResponse cancelar(Long id);
}
