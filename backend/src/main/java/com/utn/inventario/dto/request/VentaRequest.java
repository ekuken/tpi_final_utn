package com.utn.inventario.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VentaRequest {

    @Size(max = 80, message = "Máximo 80 caracteres")
    private String cliente;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Size(max = 100, message = "Máximo 100 productos por venta")
    private List<@NotNull(message = "Hay un renglón vacío") @Valid ItemVentaRequest> items;
}
