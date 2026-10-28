package com.utn.inventario.dto.response;

import com.utn.inventario.entity.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private Integer stockResultante;
    private String motivo;
    private String usuarioNombre;
    private LocalDateTime fecha;
}
