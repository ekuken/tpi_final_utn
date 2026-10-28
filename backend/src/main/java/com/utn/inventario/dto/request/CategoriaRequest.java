package com.utn.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String descripcion;
}
