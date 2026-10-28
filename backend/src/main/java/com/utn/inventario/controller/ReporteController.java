package com.utn.inventario.controller;

import com.utn.inventario.dto.response.ReporteInventarioResponse;
import com.utn.inventario.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/inventario")
    public ResponseEntity<ReporteInventarioResponse> reporteInventario() {
        return ResponseEntity.ok(reporteService.generarReporteInventario());
    }
}
