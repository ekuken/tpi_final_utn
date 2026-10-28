package com.utn.inventario.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ItemVentaRequest {

    @NotNull
    private Long productoId;

    @NotNull
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = 100000, message = "La cantidad máxima por producto es 100000")
    private Integer cantidad;
}
