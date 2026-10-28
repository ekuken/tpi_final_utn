package com.utn.inventario.controller;

import com.utn.inventario.dto.request.OrdenCompraRequest;
import com.utn.inventario.dto.response.OrdenCompraResponse;
import com.utn.inventario.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @GetMapping
    public ResponseEntity<List<OrdenCompraResponse>> listar() {
        return ResponseEntity.ok(ordenCompraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crear(@Valid @RequestBody OrdenCompraRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(ordenCompraService.crear(request, authentication.getName()));
    }

    @PostMapping("/{id}/recibir")
    public ResponseEntity<OrdenCompraResponse> recibir(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ordenCompraService.recibir(id, authentication.getName()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<OrdenCompraResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.cancelar(id));
    }
}
