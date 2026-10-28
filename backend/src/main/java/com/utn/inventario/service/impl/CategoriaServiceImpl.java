package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.CategoriaRequest;
import com.utn.inventario.dto.response.CategoriaResponse;
import com.utn.inventario.entity.Categoria;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.CategoriaRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public CategoriaResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new BusinessException("Ya existe una categoría con el nombre '" + request.getNombre() + "'");
        }
        Categoria categoria = Categoria.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return mapearAResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarPorId(id);

        categoriaRepository.findByNombreIgnoreCase(request.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new BusinessException("Ya existe una categoría con el nombre '" + request.getNombre() + "'");
            }
        });

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        return mapearAResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new BusinessException("No se puede eliminar porque tiene registros activos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + id));
    }

    private CategoriaResponse mapearAResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .build();
    }
}
