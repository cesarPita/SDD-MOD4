# Requirements — Gestión de Usuarios

## Contexto del proyecto

| Elemento           | Valor                                        |
|--------------------|----------------------------------------------|
| Framework          | Spring Boot 4.1.0                            |
| Lenguaje           | Java 25                                      |
| Build              | Maven                                        |
| Capa web           | Spring Web MVC                               |
| Persistencia       | Spring Data JPA + Hibernate                  |
| Base de datos      | PostgreSQL (localhost:5444/sdd)              |
| Utilidades         | Lombok                                       |
| Paquete base       | `com.sdd.sdd`                                |
| ddl-auto           | `none` (ya configurado — sin auto-schema)    |

### Dependencias a agregar (no existen actualmente)

- `spring-boot-starter-validation` — Bean Validation (JSR-380)
- `spring-boot-starter-security` — BCryptPasswordEncoder
- `flyway-core` + `flyway-database-postgresql` — migraciones de BD

---

## Requerimientos funcionales

### RF-01 · Registro de usuarios

| ID       | Descripción |
|----------|-------------|
| RF-01-01 | El sistema debe permitir registrar un nuevo usuario mediante `POST /api/usuarios`. |
| RF-01-02 | Los campos obligatorios son: `nombres`, `apellidos`, `username`, `email`, `password`. |
| RF-01-03 | El `username` debe ser único en el sistema. Si ya existe, el sistema debe rechazar el registro con HTTP 409. |
| RF-01-04 | El `email` debe ser único en el sistema. Si ya existe, el sistema debe rechazar el registro con HTTP 409. |
| RF-01-05 | El `email` debe tener formato válido (RFC 5322 básico). |
| RF-01-06 | La contraseña debe almacenarse cifrada con BCrypt. Nunca en texto plano. |
| RF-01-07 | Al registrar, el estado inicial del usuario será `ACTIVO`. |
| RF-01-08 | Los campos `fechaCreacion` y `fechaModificacion` se asignan automáticamente en el servidor; no los provee el cliente. |
| RF-01-09 | La respuesta de creación exitosa devuelve HTTP 201 con el `UsuarioResponse` (sin campo `password`). |

### RF-02 · Consulta de usuarios

| ID       | Descripción |
|----------|-------------|
| RF-02-01 | El sistema debe permitir obtener un usuario por su `id` mediante `GET /api/usuarios/{id}`. Si no existe, devuelve HTTP 404. |
| RF-02-02 | El sistema debe permitir obtener un usuario por `username` mediante `GET /api/usuarios/username/{username}`. Si no existe, devuelve HTTP 404. |
| RF-02-03 | El sistema debe permitir listar usuarios con paginación mediante `GET /api/usuarios`. |
| RF-02-04 | El listado soporta los siguientes filtros opcionales como query params: `username`, `email`, `estado`. |
| RF-02-05 | El listado soporta los parámetros de paginación estándar de Spring: `page`, `size`, `sort`. |
| RF-02-06 | Ninguna respuesta de consulta incluye el campo `password`. |

### RF-03 · Edición de usuarios

| ID       | Descripción |
|----------|-------------|
| RF-03-01 | El sistema debe permitir modificar un usuario existente mediante `PUT /api/usuarios/{id}`. |
| RF-03-02 | Los campos modificables son: `nombres`, `apellidos`, `email`, `estado`. |
| RF-03-03 | El `username` no es modificable una vez creado. |
| RF-03-04 | Si se cambia el `email` a uno ya registrado en otro usuario, el sistema rechaza con HTTP 409. |
| RF-03-05 | El campo `fechaModificacion` se actualiza automáticamente en cada edición. |
| RF-03-06 | Si el usuario no existe, devuelve HTTP 404. |
| RF-03-07 | La respuesta exitosa devuelve HTTP 200 con el `UsuarioResponse` actualizado. |

### RF-04 · Eliminación lógica de usuarios

| ID       | Descripción |
|----------|-------------|
| RF-04-01 | El sistema debe implementar eliminación lógica mediante `DELETE /api/usuarios/{id}`. |
| RF-04-02 | La eliminación lógica cambia el campo `estado` a `INACTIVO`. No elimina el registro físico. |
| RF-04-03 | El campo `fechaModificacion` se actualiza al momento de la eliminación lógica. |
| RF-04-04 | Si el usuario no existe, devuelve HTTP 404. |
| RF-04-05 | La respuesta exitosa devuelve HTTP 204 (sin cuerpo). |

### RF-05 · Base de datos y migraciones

| ID       | Descripción |
|----------|-------------|
| RF-05-01 | La estructura de tablas se gestiona exclusivamente mediante Flyway. No se usa `ddl-auto=create` ni `update`. |
| RF-05-02 | Debe existir al menos el script `V1__create_usuarios_table.sql` como primera migración. |
| RF-05-03 | La tabla `usuarios` incluye restricciones `UNIQUE` sobre `username` y `email`. |
| RF-05-04 | Se crean índices sobre `username`, `email` y `estado` para optimizar consultas frecuentes. |
| RF-05-05 | La clave primaria `id` es de tipo `BIGSERIAL` (auto-incremental). |
| RF-05-06 | Los campos `fecha_creacion` y `fecha_modificacion` son `TIMESTAMP WITH TIME ZONE NOT NULL`. |

### RF-06 · API REST

| Método | Endpoint                              | Descripción                         |
|--------|---------------------------------------|-------------------------------------|
| POST   | `/api/usuarios`                       | Registrar usuario                   |
| GET    | `/api/usuarios`                       | Listar usuarios (paginado/filtrado) |
| GET    | `/api/usuarios/{id}`                  | Obtener usuario por ID              |
| GET    | `/api/usuarios/username/{username}`   | Obtener usuario por username        |
| PUT    | `/api/usuarios/{id}`                  | Actualizar usuario                  |
| DELETE | `/api/usuarios/{id}`                  | Eliminación lógica                  |

### RF-07 · DTOs y respuestas

| ID       | Descripción |
|----------|-------------|
| RF-07-01 | Las peticiones de creación y edición usan DTOs separados (`UsuarioRequest`, `UsuarioUpdateRequest`). |
| RF-07-02 | Las respuestas usan `UsuarioResponse`. Este DTO nunca incluye el campo `password`. |
| RF-07-03 | El listado paginado se envuelve en `PageResponse<UsuarioResponse>` con metadatos de paginación. |
| RF-07-04 | Los errores se devuelven en una estructura uniforme `ApiError` con `timestamp`, `status`, `error`, `mensaje` y `detalle`. |
| RF-07-05 | Los errores de validación (HTTP 400) incluyen la lista de campos inválidos y sus mensajes. |

### RF-08 · Seguridad

| ID       | Descripción |
|----------|-------------|
| RF-08-01 | Las contraseñas se cifran con `BCryptPasswordEncoder` antes de persistir. |
| RF-08-02 | La contraseña nunca se registra en logs (no hay `toString()` del campo en entidades ni DTOs de request). |
| RF-08-03 | La contraseña nunca se devuelve en respuestas. |
| RF-08-04 | Las credenciales de BD se leen de `application.properties` / variables de entorno; no están hardcodeadas en código fuente. |
| RF-08-05 | Los datos de entrada se validan y sanitizan mediante Bean Validation antes de procesarse. |

### RF-09 · Auditoría

| ID       | Descripción |
|----------|-------------|
| RF-09-01 | `fechaCreacion` se asigna automáticamente al persistir por primera vez (JPA Auditing `@CreatedDate`). |
| RF-09-02 | `fechaModificacion` se actualiza automáticamente en cada operación de escritura (`@LastModifiedDate`). |
| RF-09-03 | Si se implementa contexto de seguridad en el futuro, los campos `creadoPor` y `modificadoPor` pueden activarse mediante `@CreatedBy` / `@LastModifiedBy`. La entidad los reserva como campos opcionales comentados. |

---

## Requerimientos no funcionales

| ID      | Descripción |
|---------|-------------|
| RNF-01  | El módulo no debe romper la estructura ni convenciones del proyecto base. |
| RNF-02  | Solo se modifican o crean archivos directamente relacionados con Gestión de Usuarios y la configuración mínima necesaria (pom.xml, application.properties, Flyway). |
| RNF-03  | El código debe ser legible, sin lógica de negocio en el controlador. |
| RNF-04  | El manejo de errores debe ser centralizado (`@ControllerAdvice`). |
| RNF-05  | Las pruebas deben cubrir los escenarios mínimos definidos en la sección de Testing. |

---

## Escenarios de prueba requeridos

| ID    | Escenario                                           | Resultado esperado                    |
|-------|-----------------------------------------------------|---------------------------------------|
| T-01  | Registro exitoso con datos válidos                  | HTTP 201, UsuarioResponse sin password |
| T-02  | Registro con username duplicado                     | HTTP 409, ApiError                    |
| T-03  | Registro con email duplicado                        | HTTP 409, ApiError                    |
| T-04  | Registro con datos inválidos (email mal formado)    | HTTP 400, ApiError con detalle        |
| T-05  | Registro con campos obligatorios vacíos             | HTTP 400, ApiError con detalle        |
| T-06  | Consulta por ID existente                           | HTTP 200, UsuarioResponse             |
| T-07  | Consulta por ID inexistente                         | HTTP 404, ApiError                    |
| T-08  | Consulta por username existente                     | HTTP 200, UsuarioResponse             |
| T-09  | Consulta por username inexistente                   | HTTP 404, ApiError                    |
| T-10  | Modificación exitosa                                | HTTP 200, UsuarioResponse actualizado |
| T-11  | Modificación de usuario inexistente                 | HTTP 404, ApiError                    |
| T-12  | Modificación con email duplicado                    | HTTP 409, ApiError                    |
| T-13  | Eliminación lógica de usuario existente             | HTTP 204, estado INACTIVO en BD       |
| T-14  | Eliminación lógica de usuario inexistente           | HTTP 404, ApiError                    |
| T-15  | Listado paginado con parámetros válidos             | HTTP 200, PageResponse con metadatos  |
| T-16  | Filtro por estado ACTIVO                            | HTTP 200, solo usuarios activos       |
| T-17  | Filtro por username parcial                         | HTTP 200, resultados filtrados        |
