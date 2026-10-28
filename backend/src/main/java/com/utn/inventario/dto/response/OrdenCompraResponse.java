package com.utn.inventario.dto.response;

import com.utn.inventario.entity.EstadoOrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponse {
    private Long id;
    private Long proveedorId;
    private String proveedorNombre;
    private EstadoOrdenCompra estado;
    private String observaciones;
    private BigDecimal total;
    private String usuarioNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRecepcion;
    private List<ItemDetalleResponse> items;
}
