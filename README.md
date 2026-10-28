# Sistema de Gestión de Inventario

Trabajo Práctico Final — Tecnicatura Universitaria en Programación (UTN Villa María)

**Integrantes:** Santiago Antón (legajo 17379) y Elías Benejam (legajo 17362)
**Tutor académico:** Juan Pablo Dealbera

## Descripción

Aplicación web para la gestión de inventario de un comercio. Permite administrar
productos, categorías y proveedores, registrar entradas y salidas de stock,
consultar el historial de movimientos y obtener reportes básicos del estado
del inventario, con autenticación de usuarios y dos niveles de acceso
(administrador y empleado).

## Tecnologías utilizadas

- **Backend:** Java 17+ · Spring Boot 3 · Spring Data JPA (Hibernate) · Spring Security · JWT
- **Base de datos:** PostgreSQL en la nube con [Supabase](https://supabase.com) · H2 en memoria (solo para pruebas sin conexión)
- **Frontend:** HTML, CSS y JavaScript (sin frameworks), consumiendo la API REST vía `fetch`
- **Build tool:** Maven
- **Documentación de la API:** Swagger / OpenAPI (`springdoc-openapi`)

## Estructura del proyecto

```
inventario-app/
├── backend/                # API REST en Spring Boot
│   ├── .env.example        # Plantilla de credenciales de Supabase (copiar como .env)
│   ├── src/main/java/com/utn/inventario/
│   │   ├── config/         # Seguridad, CORS, Swagger, carga de datos iniciales
│   │   ├── controller/     # Endpoints REST
│   │   ├── dto/             # Objetos de request/response
│   │   ├── entity/          # Entidades JPA
│   │   ├── exception/       # Manejo centralizado de errores
│   │   ├── repository/      # Repositorios Spring Data JPA
│   │   ├── security/        # JWT y detalles de usuario
│   │   └── service/         # Lógica de negocio
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-supabase.properties (perfil Supabase / PostgreSQL, por defecto)
│       └── application-dev.properties      (perfil H2 en memoria, para pruebas)
├── database/
│   └── schema.sql          # Script de creación de tablas para Supabase (PostgreSQL)
└── frontend/                # Sitio estático (HTML/CSS/JS)
```

## Cómo ejecutar el backend

### 1. Crear la base de datos en Supabase (una sola vez)

1. Crear un proyecto en [supabase.com](https://supabase.com) (región recomendada: São Paulo)
   y guardar la contraseña de la base de datos.
2. Ir a **SQL Editor > New query**, pegar el contenido de `database/schema.sql` y ejecutarlo
   con **Run**. Crea las tablas y activa Row Level Security, para que las tablas no queden
   expuestas en la API REST pública que Supabase genera automáticamente.

### 2. Configurar las credenciales

1. Copiar `backend/.env.example` como `backend/.env`.
2. En Supabase, tocar **Connect**, pestaña **JDBC**, opción **Session pooler** (puerto 5432),
   y completar en `backend/.env`:

| Variable               | Ejemplo                                                                              |
|------------------------|--------------------------------------------------------------------------------------|
| `SUPABASE_DB_URL`      | `jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USER`     | `postgres.abcdefghijklmnop` (incluye el id del proyecto)                              |
| `SUPABASE_DB_PASSWORD` | la contraseña de la base de datos                                                    |

> No usar la "Direct connection" (`db.xxx.supabase.co`): solo funciona con IPv6.
> El archivo `.env` contiene la contraseña: no se sube al repositorio (está en `.gitignore`).
> También se pueden definir esas tres variables como variables de entorno del sistema.

### 3. Ejecutar

```bash
cd backend
mvn spring-boot:run
```

O desde el IDE, ejecutando `InventarioApplication`. El perfil `supabase` es el perfil por defecto.

La API queda disponible en `http://localhost:8080/api` y la documentación interactiva en
`http://localhost:8080/swagger-ui.html`. Al arrancar por primera vez se crean automáticamente:

- 2 usuarios de prueba (ver más abajo)
- 3 categorías, 2 proveedores y 5 productos de ejemplo

Los datos quedan guardados de forma permanente en Supabase.

**Errores frecuentes:**
- `Driver org.postgresql.Driver claims to not accept jdbcUrl, ${SUPABASE_DB_URL}`: falta el archivo `backend/.env` o está mal completado.
- `tenant/user ... not found`: el usuario no tiene el formato `postgres.<id-del-proyecto>`.
- `password authentication failed`: contraseña incorrecta (se puede resetear en Project Settings > Database).
- `Port 8080 was already in use`: ya hay otra instancia de la aplicación corriendo.

### Opción sin conexión (H2 en memoria)

Para probar sin Supabase, ejecutar con el perfil `dev`:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> Con el perfil `dev` los datos se pierden al reiniciar la aplicación (base en memoria).

### Usuarios iniciales

| Rol        | Email                  | Contraseña   |
|------------|-------------------------|--------------|
| ADMIN      | admin@inventario.com    | admin123     |
| EMPLEADO   | empleado@inventario.com | empleado123  |

También se puede crear una cuenta nueva desde la pantalla de login del frontend
(siempre se crea con rol EMPLEADO; los administradores adicionales se crean
desde la sección "Usuarios", que solo ve un ADMIN).

## Tests automáticos

```bash
cd backend
mvn test
```

Corren contra una base H2 en memoria (perfil `test`), así que no tocan Supabase ni necesitan
credenciales. Son 70 tests en `backend/src/test`:

- **Unitarios** (JUnit + Mockito): reglas de stock y cálculo del margen.
- **De integración** (`@SpringBootTest` + MockMvc): hacen pedidos HTTP reales con token JWT a toda
  la API: login y permisos, usuarios, categorías, proveedores, productos, movimientos, ventas
  (incluidas ventas simultáneas del último producto), órdenes de compra y reportes. También prueban
  pedidos mal armados (JSON inválido, tipos incorrectos, rutas inexistentes) para que nunca den error 500.

## Cómo ejecutar el frontend

El frontend es HTML/CSS/JS plano, sin build ni dependencias. Para evitar problemas
de CORS al abrir los archivos directamente (`file://`), se recomienda servirlo con
un servidor HTTP simple:

```bash
cd frontend
python3 -m http.server 5500
```

Y abrir `http://localhost:5500` en el navegador (con el backend corriendo en el
puerto 8080 en paralelo). Si el backend corre en otra URL, ajustar `API_BASE_URL`
en `frontend/js/config.js`.

## Endpoints principales de la API

Todos los endpoints (salvo `/api/auth/**`) requieren el header
`Authorization: Bearer <token>`, obtenido al hacer login.

| Método | Endpoint                          | Descripción                              | Acceso        |
|--------|------------------------------------|-------------------------------------------|---------------|
| POST   | `/api/auth/login`                  | Inicia sesión, devuelve JWT               | Público       |
| POST   | `/api/auth/registro`               | Autoregistro (crea usuario EMPLEADO)      | Público       |
| GET    | `/api/usuarios`                    | Lista usuarios                            | ADMIN         |
| POST   | `/api/usuarios`                    | Crea usuario (puede asignar rol)          | ADMIN         |
| PATCH  | `/api/usuarios/{id}/estado`        | Activa/desactiva un usuario               | ADMIN         |
| DELETE | `/api/usuarios/{id}`               | Elimina un usuario                        | ADMIN         |
| GET/POST/PUT/DELETE | `/api/categorias[/{id}]` | CRUD de categorías                        | Autenticado   |
| GET/POST/PUT/DELETE | `/api/proveedores[/{id}]`| CRUD de proveedores                       | Autenticado   |
| GET/POST/PUT/DELETE | `/api/productos[/{id}]`  | CRUD de productos                         | Autenticado   |
| GET    | `/api/productos/stock-bajo`        | Productos con stock ≤ stock mínimo        | Autenticado   |
| POST   | `/api/movimientos/entrada`         | Registra una entrada de stock             | Autenticado   |
| POST   | `/api/movimientos/salida`          | Registra una salida de stock (valida stock disponible) | Autenticado |
| GET    | `/api/movimientos`                 | Historial completo de movimientos         | Autenticado   |
| GET    | `/api/movimientos/producto/{id}`   | Historial de un producto puntual          | Autenticado   |
| GET    | `/api/ventas[/{id}]`               | Lista ventas / detalle de una venta       | Autenticado   |
| POST   | `/api/ventas`                      | Registra una venta con varios productos (descuenta stock) | Autenticado |
| POST   | `/api/ventas/{id}/anular`          | Anula una venta y devuelve el stock       | ADMIN         |
| GET    | `/api/ordenes-compra[/{id}]`       | Lista órdenes / detalle de una orden      | Autenticado   |
| POST   | `/api/ordenes-compra`              | Crea una orden de compra (queda PENDIENTE)| Autenticado   |
| POST   | `/api/ordenes-compra/{id}/recibir` | Recibe la mercadería: suma stock y actualiza el costo | Autenticado |
| POST   | `/api/ordenes-compra/{id}/cancelar`| Cancela una orden pendiente               | Autenticado   |
| GET    | `/api/reportes/inventario`         | Totales, valorización a costo y venta, ventas del mes, stock bajo | Autenticado |

## Modelo de datos (resumen)

- **Usuario**: nombre, email, password (hasheada con BCrypt), rol (ADMIN/EMPLEADO), activo
- **Categoría**: nombre, descripción
- **Proveedor**: nombre, contacto, teléfono, email, dirección
- **Producto**: código, nombre, descripción, precio de venta, precio de costo, stock, stock mínimo, categoría, proveedor, versión (bloqueo optimista)
- **MovimientoStock**: producto, tipo (ENTRADA/SALIDA), cantidad, stock resultante, motivo, usuario, fecha
- **OrdenCompra**: proveedor, estado (PENDIENTE/RECIBIDA/CANCELADA), observaciones, total, usuario, fechas de creación y recepción, ítems (producto, cantidad, costo unitario)
- **Venta**: cliente, estado (CONFIRMADA/ANULADA), total, usuario, fecha, ítems (producto, cantidad, precio unitario)

El script completo de creación de tablas (PostgreSQL / Supabase) está en `database/schema.sql`.

## Reglas de negocio implementadas

- No se puede registrar una **salida** de stock mayor al stock disponible (error 400 con mensaje claro).
- Los códigos de producto y los nombres de categoría no se pueden repetir (sin importar mayúsculas: "ab-1" y "AB-1" son el mismo código).
- Los emails se guardan en minúsculas: "Ana@Mail.com" y "ana@mail.com" son la misma cuenta.
- El stock nunca puede desbordarse: hay topes por movimiento (100.000) y por stock inicial (1.000.000).
- Dar de baja un producto es una baja lógica (`activo = false`), para no perder el historial de movimientos asociado.
- Solo un usuario con rol **ADMIN** puede gestionar usuarios.
- El registro público de usuarios nunca asigna el rol ADMIN (evita que cualquiera se autoasigne permisos de administrador).
- No se puede eliminar una categoría o proveedor con productos asociados (ni un proveedor con órdenes de compra), ni un usuario con movimientos, ventas u órdenes registradas.
- Una **venta** es todo o nada: si algún producto no tiene stock suficiente no se registra nada. Cada renglón genera una salida de stock con el motivo "Venta #N".
- El precio de cada renglón de la venta queda guardado: si después cambia el precio del producto, la venta no se modifica.
- Solo un **ADMIN** puede anular una venta. Al anularla se devuelve el stock ("Anulación de venta #N").
- Una **orden de compra** no modifica el stock hasta que se marca como recibida. Al recibirla se suma el stock y el precio de costo de cada producto pasa a ser el de la orden.
- Solo las órdenes pendientes se pueden recibir o cancelar.
- Ni las ventas ni las órdenes pueden repetir un producto en dos renglones.
- El menú muestra cuántos productos tienen stock bajo (en rojo si alguno se quedó sin stock). Desde el panel, "Pedir al proveedor" arma una orden de compra con los productos faltantes de ese proveedor.
- Un administrador no puede desactivarse ni eliminarse a sí mismo.
- Si un administrador desactiva a un usuario, su sesión (token JWT) deja de funcionar inmediatamente.
- Dos movimientos simultáneos sobre el mismo producto no pueden dejar el stock inconsistente (bloqueo optimista con `@Version`).

## Próximos pasos sugeridos (para las siguientes entregas)

- Paginación en los listados cuando el volumen de datos crezca.
- Exportación de reportes a PDF/Excel.
- Aviso de stock bajo por email (requiere configurar un servidor SMTP).
