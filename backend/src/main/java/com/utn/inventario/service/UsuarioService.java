package com.utn.inventario.service;

import com.utn.inventario.dto.request.RegistroUsuarioRequest;
import com.utn.inventario.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponse> listar();
    UsuarioResponse obtenerPorId(Long id);
    UsuarioResponse crear(RegistroUsuarioRequest request);
    UsuarioResponse cambiarEstado(Long id, boolean activo);
    void eliminar(Long id);
}
