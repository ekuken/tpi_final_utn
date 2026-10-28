package com.utn.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @Size(max = 80, message = "Máximo 80 caracteres")
    private String contacto;
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String telefono;
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String email;
    @Size(max = 150, message = "Máximo 150 caracteres")
    private String direccion;
}
