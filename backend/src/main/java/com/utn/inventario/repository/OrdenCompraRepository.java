package com.utn.inventario.repository;

import com.utn.inventario.entity.EstadoOrdenCompra;
import com.utn.inventario.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    @Query("SELECT DISTINCT o FROM OrdenCompra o JOIN FETCH o.proveedor LEFT JOIN FETCH o.usuario "
            + "LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto ORDER BY o.fechaCreacion DESC")
    List<OrdenCompra> findAllConDetalle();

    @Query("SELECT o FROM OrdenCompra o JOIN FETCH o.proveedor LEFT JOIN FETCH o.usuario "
            + "LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.producto WHERE o.id = :id")
    Optional<OrdenCompra> findByIdConDetalle(@Param("id") Long id);

    long countByEstado(EstadoOrdenCompra estado);

    boolean existsByProveedorId(Long proveedorId);

    boolean existsByUsuarioId(Long usuarioId);
}
