package com.utn.inventario.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrdenCompraRequest {

    @NotNull(message = "Seleccioná un proveedor")
    private Long proveedorId;

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String observaciones;

    @NotEmpty(message = "La orden debe tener al menos un producto")
    @Size(max = 100, message = "Máximo 100 productos por orden")
    private List<@NotNull(message = "Hay un renglón vacío") @Valid ItemOrdenCompraRequest> items;
}
