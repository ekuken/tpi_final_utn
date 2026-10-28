package com.utn.inventario.repository;

import com.utn.inventario.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByProductoIdOrderByFechaDesc(Long productoId);

    List<MovimientoStock> findAllByOrderByFechaDesc();

    boolean existsByUsuarioId(Long usuarioId);
}
