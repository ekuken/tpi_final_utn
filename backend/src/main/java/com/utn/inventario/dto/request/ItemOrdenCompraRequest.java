package com.utn.inventario.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemOrdenCompraRequest {

    @NotNull
    private Long productoId;

    @NotNull
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = 100000, message = "La cantidad máxima por producto es 100000")
    private Integer cantidad;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Máximo 8 dígitos enteros y 2 decimales")
    private BigDecimal costoUnitario;
}
