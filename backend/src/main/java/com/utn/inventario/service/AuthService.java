package com.utn.inventario.service;

import com.utn.inventario.dto.request.LoginRequest;
import com.utn.inventario.dto.request.RegistroUsuarioRequest;
import com.utn.inventario.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse registrar(RegistroUsuarioRequest request);
}
