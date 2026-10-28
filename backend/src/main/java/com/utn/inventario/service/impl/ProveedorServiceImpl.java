package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.ProveedorRequest;
import com.utn.inventario.dto.response.ProveedorResponse;
import com.utn.inventario.entity.Proveedor;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.OrdenCompraRepository;
import com.utn.inventario.repository.ProductoRepository;
import com.utn.inventario.repository.ProveedorRepository;
import com.utn.inventario.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    @Override
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public ProveedorResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor proveedor = Proveedor.builder()
                .nombre(request.getNombre())
                .contacto(request.getContacto())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .direccion(request.getDireccion())
                .build();
        return mapearAResponse(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setNombre(request.getNombre());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setDireccion(request.getDireccion());
        return mapearAResponse(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscarPorId(id);
        if (productoRepository.existsByProveedorId(id) || ordenCompraRepository.existsByProveedorId(id)) {
            throw new BusinessException("No se puede eliminar porque tiene registros activos asociados");
        }
        proveedorRepository.delete(proveedor);
    }

    private Proveedor buscarPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id " + id));
    }

    private ProveedorResponse mapearAResponse(Proveedor proveedor) {
        return ProveedorResponse.builder()
                .id(proveedor.getId())
                .nombre(proveedor.getNombre())
                .contacto(proveedor.getContacto())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .direccion(proveedor.getDireccion())
                .build();
    }
}
