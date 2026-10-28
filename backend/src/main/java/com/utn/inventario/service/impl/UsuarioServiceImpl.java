package com.utn.inventario.service.impl;

import com.utn.inventario.dto.request.RegistroUsuarioRequest;
import com.utn.inventario.dto.response.UsuarioResponse;
import com.utn.inventario.entity.RolUsuario;
import com.utn.inventario.entity.Usuario;
import com.utn.inventario.exception.BusinessException;
import com.utn.inventario.exception.ResourceNotFoundException;
import com.utn.inventario.repository.MovimientoStockRepository;
import com.utn.inventario.repository.OrdenCompraRepository;
import com.utn.inventario.repository.UsuarioRepository;
import com.utn.inventario.repository.VentaRepository;
import com.utn.inventario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final VentaRepository ventaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public UsuarioResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(RegistroUsuarioRequest request) {
        String email = Usuario.normalizarEmail(request.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un usuario registrado con ese email");
        }
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol() != null ? request.getRol() : RolUsuario.EMPLEADO)
                .activo(true)
                .build();
        return mapearAResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscarPorId(id);
        validarQueNoSeaElUsuarioLogueado(usuario);
        usuario.setActivo(activo);
        return mapearAResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscarPorId(id);
        validarQueNoSeaElUsuarioLogueado(usuario);
        if (movimientoRepository.existsByUsuarioId(id)
                || ventaRepository.existsByUsuarioId(id)
                || ordenCompraRepository.existsByUsuarioId(id)) {
            throw new BusinessException("No se puede eliminar porque tiene registros activos asociados");
        }
        usuarioRepository.delete(usuario);
    }

    private void validarQueNoSeaElUsuarioLogueado(Usuario usuario) {
        String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (usuario.getEmail().equalsIgnoreCase(emailLogueado)) {
            throw new BusinessException("No puedes modificar tu propio estado desde esta vista");
        }
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private UsuarioResponse mapearAResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
