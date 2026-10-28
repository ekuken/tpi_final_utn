package com.utn.inventario.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteInventarioResponse {
    private long totalProductos;
    private long totalUnidadesEnStock;
    private BigDecimal valorTotalInventario;
    private BigDecimal valorInventarioCosto;
    private BigDecimal gananciaPotencial;
    private long productosConStockBajo;
    private long productosSinStock;
    private List<ProductoResponse> detalleStockBajo;
    private BigDecimal ventasDelMes;
    private long cantidadVentasDelMes;
    private long ordenesCompraPendientes;
}
