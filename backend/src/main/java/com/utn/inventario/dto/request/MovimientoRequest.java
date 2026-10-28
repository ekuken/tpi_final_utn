package com.utn.inventario.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MovimientoRequest {

    @NotNull
    private Long productoId;

    @NotNull
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = 100000, message = "La cantidad máxima por movimiento es 100000")
    private Integer cantidad;

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String motivo;
}
