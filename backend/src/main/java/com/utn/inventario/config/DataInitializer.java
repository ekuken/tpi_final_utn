package com.utn.inventario.config;

import com.utn.inventario.entity.*;
import com.utn.inventario.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            crearUsuarioAdmin();
        }

        if (categoriaRepository.count() == 0 && proveedorRepository.count() == 0 && productoRepository.count() == 0) {
            crearDatosDeEjemplo();
        }
    }

    private void crearUsuarioAdmin() {
        Usuario admin = Usuario.builder()
                .nombre("Administrador")
                .email("admin@inventario.com")
                .password(passwordEncoder.encode("admin123"))
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build();
        usuarioRepository.save(admin);

        Usuario empleado = Usuario.builder()
                .nombre("Empleado Demo")
                .email("empleado@inventario.com")
                .password(passwordEncoder.encode("empleado123"))
                .rol(RolUsuario.EMPLEADO)
                .activo(true)
                .build();
        usuarioRepository.save(empleado);

        log.info("=======================================================");
        log.info(" Usuarios iniciales creados:");
        log.info("  ADMIN     -> admin@inventario.com / admin123");
        log.info("  EMPLEADO  -> empleado@inventario.com / empleado123");
        log.info("=======================================================");
    }

    private void crearDatosDeEjemplo() {
        Categoria bebidas = categoriaRepository.save(Categoria.builder()
                .nombre("Bebidas").descripcion("Bebidas con y sin alcohol").build());
        Categoria almacen = categoriaRepository.save(Categoria.builder()
                .nombre("Almacén").descripcion("Productos secos y no perecederos").build());
        Categoria limpieza = categoriaRepository.save(Categoria.builder()
                .nombre("Limpieza").descripcion("Articulos de limpieza").build());

        Proveedor distribuidoraSur = proveedorRepository.save(Proveedor.builder()
                .nombre("Distribuidora Sur S.A.")
                .contacto("Marcos Gomez")
                .telefono("353-400-1111")
                .email("ventas@distribuidorasur.com")
                .direccion("Ruta 9 Km 560, Villa María")
                .build());

        Proveedor mayoristaCentro = proveedorRepository.save(Proveedor.builder()
                .nombre("Mayorista del Centro")
                .contacto("Laura Perez")
                .telefono("353-400-2222")
                .email("contacto@mayoristacentro.com")
                .direccion("Av. Sabattini 1200, Villa María")
                .build());

        productoRepository.save(Producto.builder()
                .codigo("BEB-001").nombre("Agua mineral 500ml").descripcion("Botella individual")
                .precio(new BigDecimal("800.00")).precioCosto(new BigDecimal("520.00")).stock(120).stockMinimo(20)
                .categoria(bebidas).proveedor(distribuidoraSur).activo(true).build());

        productoRepository.save(Producto.builder()
                .codigo("BEB-002").nombre("Gaseosa cola 1.5L").descripcion("Botella retornable")
                .precio(new BigDecimal("1500.00")).precioCosto(new BigDecimal("1050.00")).stock(15).stockMinimo(20)
                .categoria(bebidas).proveedor(distribuidoraSur).activo(true).build());

        productoRepository.save(Producto.builder()
                .codigo("ALM-001").nombre("Arroz 1kg").descripcion("Arroz largo fino")
                .precio(new BigDecimal("1200.00")).precioCosto(new BigDecimal("850.00")).stock(60).stockMinimo(10)
                .categoria(almacen).proveedor(mayoristaCentro).activo(true).build());

        productoRepository.save(Producto.builder()
                .codigo("ALM-002").nombre("Fideos 500g").descripcion("Fideos tipo tallarín")
                .precio(new BigDecimal("900.00")).precioCosto(new BigDecimal("600.00")).stock(8).stockMinimo(15)
                .categoria(almacen).proveedor(mayoristaCentro).activo(true).build());

        productoRepository.save(Producto.builder()
                .codigo("LIM-001").nombre("Detergente 750ml").descripcion("Lavavajillas")
                .precio(new BigDecimal("1100.00")).precioCosto(new BigDecimal("700.00")).stock(40).stockMinimo(10)
                .categoria(limpieza).proveedor(distribuidoraSur).activo(true).build());

        log.info("Datos de ejemplo cargados: 3 categorías, 2 proveedores, 5 productos.");
    }
}
