package com.utn.inventario.controller;

import com.utn.inventario.dto.request.MovimientoRequest;
import com.utn.inventario.dto.response.MovimientoResponse;
import com.utn.inventario.service.MovimientoStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoStockService movimientoStockService;

    @PostMapping("/entrada")
    public ResponseEntity<MovimientoResponse> registrarEntrada(@Valid @RequestBody MovimientoRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(movimientoStockService.registrarEntrada(request, authentication.getName()));
    }

    @PostMapping("/salida")
    public ResponseEntity<MovimientoResponse> registrarSalida(@Valid @RequestBody MovimientoRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(movimientoStockService.registrarSalida(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<MovimientoResponse>> listarTodos() {
        return ResponseEntity.ok(movimientoStockService.listarTodos());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<MovimientoResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(movimientoStockService.listarPorProducto(productoId));
    }
}
