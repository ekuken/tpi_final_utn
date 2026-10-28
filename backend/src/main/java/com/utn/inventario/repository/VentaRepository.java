package com.utn.inventario.repository;

import com.utn.inventario.entity.EstadoVenta;
import com.utn.inventario.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.usuario "
            + "LEFT JOIN FETCH v.items i LEFT JOIN FETCH i.producto ORDER BY v.fecha DESC")
    List<Venta> findAllConDetalle();

    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.usuario "
            + "LEFT JOIN FETCH v.items i LEFT JOIN FETCH i.producto WHERE v.id = :id")
    Optional<Venta> findByIdConDetalle(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.estado = :estado AND v.fecha >= :desde")
    BigDecimal sumarTotalDesde(@Param("estado") EstadoVenta estado, @Param("desde") LocalDateTime desde);

    long countByEstadoAndFechaGreaterThanEqual(EstadoVenta estado, LocalDateTime desde);

    boolean existsByUsuarioId(Long usuarioId);
}
