package com.utn.inventario.dto.request;

import com.utn.inventario.entity.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroUsuarioRequest {

    @NotBlank
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String email;

    @NotBlank
    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    private String password;

    private RolUsuario rol;
}
