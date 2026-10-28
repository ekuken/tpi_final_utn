package com.utn.inventario.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    @NotBlank
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String codigo;

    @NotBlank
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Máximo 8 dígitos enteros y 2 decimales")
    private BigDecimal precio;

    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de costo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Máximo 8 dígitos enteros y 2 decimales")
    private BigDecimal precioCosto;

    @PositiveOrZero
    @Max(value = 1000000, message = "El stock inicial máximo es 1000000")
    private Integer stockInicial;

    @PositiveOrZero
    @Max(value = 1000000, message = "El stock mínimo máximo es 1000000")
    private Integer stockMinimo;

    private Long categoriaId;

    private Long proveedorId;
}
