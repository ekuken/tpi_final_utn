package com.utn.inventario.service.impl;

import com.utn.inventario.dto.response.ProductoResponse;
import com.utn.inventario.entity.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMapperTest {

    @Test
    void margenEsLaGananciaSobreElCosto() {
        assertThat(ProductoMapper.calcularMargen(new BigDecimal("150"), new BigDecimal("100")))
                .isEqualByComparingTo("50.0");
    }

    @Test
    void margenSeRedondeaAUnDecimal() {
        assertThat(ProductoMapper.calcularMargen(new BigDecimal("800"), new BigDecimal("520")))
                .isEqualByComparingTo("53.8");
    }

    @Test
    void margenEsNegativoSiSeVendePorDebajoDelCosto() {
        assertThat(ProductoMapper.calcularMargen(new BigDecimal("80"), new BigDecimal("100")))
                .isEqualByComparingTo("-20.0");
    }

    @Test
    void sinCostoNoHayMargen() {
        assertThat(ProductoMapper.calcularMargen(new BigDecimal("100"), BigDecimal.ZERO)).isNull();
        assertThat(ProductoMapper.calcularMargen(new BigDecimal("100"), null)).isNull();
    }

    @Test
    void marcaStockBajoCuandoElStockLlegaAlMinimo() {
        Producto p = Producto.builder().codigo("X").nombre("X")
                .precio(BigDecimal.TEN).precioCosto(BigDecimal.ONE).stock(5).stockMinimo(5).build();

        ProductoResponse r = ProductoMapper.toResponse(p);

        assertThat(r.isStockBajo()).isTrue();
        assertThat(r.getCategoriaNombre()).isNull();
        assertThat(r.getProveedorNombre()).isNull();
        assertThat(r.getMargen()).isEqualByComparingTo("900.0");
    }
}
