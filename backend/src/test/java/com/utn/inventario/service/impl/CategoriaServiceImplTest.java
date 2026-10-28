package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.CategoriaRequest;
import com.utn.inventario.dto.response.CategoriaResponse;
import com.utn.inventario.entity.Categoria;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.repository.CategoriaRepository;
import com.utn.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CategoriaServiceImpl servicio;

    private static CategoriaRequest request(String nombre) {
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre(nombre);
        request.setDescripcion("desc");
        return request;
    }

    @Test
    void crearConNombreDuplicadoEnOtrasMayusculasLanzaExcepcion() {
        when(categoriaRepository.existsByNombreIgnoreCase("BEBIDAS")).thenReturn(true);
        CategoriaRequest pedido = request("BEBIDAS");

        assertThatThrownBy(() -> servicio.crear(pedido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BEBIDAS");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizarConservandoSuPropioNombreNoFalla() {
        Categoria bebidas = Categoria.builder().id(1L).nombre("Bebidas").descripcion("vieja").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(bebidas));
        when(categoriaRepository.findByNombreIgnoreCase("Bebidas")).thenReturn(Optional.of(bebidas));
        when(categoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse respuesta = servicio.actualizar(1L, request("Bebidas"));

        assertThat(respuesta.getNombre()).isEqualTo("Bebidas");
        assertThat(respuesta.getDescripcion()).isEqualTo("desc");
    }

    @Test
    void actualizarConElNombreDeOtraCategoriaLanzaExcepcion() {
        Categoria bebidas = Categoria.builder().id(1L).nombre("Bebidas").build();
        Categoria limpieza = Categoria.builder().id(2L).nombre("Limpieza").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(bebidas));
        when(categoriaRepository.findByNombreIgnoreCase("limpieza")).thenReturn(Optional.of(limpieza));
        CategoriaRequest pedido = request("limpieza");

        assertThatThrownBy(() -> servicio.actualizar(1L, pedido))
                .isInstanceOf(BusinessException.class);
        verify(categoriaRepository, never()).save(any());
    }
}
