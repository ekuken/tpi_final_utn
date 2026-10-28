package com.utn.inventario.repository;

import com.utn.inventario.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    List<Producto> findByCategoriaId(Long categoriaId);

    List<Producto> findByProveedorId(Long proveedorId);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByProveedorId(Long proveedorId);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo")
    List<Producto> findConStockBajo();

    @Query("SELECT p FROM Producto p WHERE p.activo = true")
    List<Producto> findAllActivos();
}
