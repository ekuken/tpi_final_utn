package com.utn.inventario.dto.response;

import com.utn.inventario.entity.EstadoVenta;
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
public class VentaResponse {
    private Long id;
    private String cliente;
    private EstadoVenta estado;
    private BigDecimal total;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private List<ItemDetalleResponse> items;
}
