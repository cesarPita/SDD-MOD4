# Design — Gestión de Usuarios

## 1. Visión general de la arquitectura

El módulo sigue una arquitectura en capas estricta. Cada capa solo conoce a la inmediatamente inferior. La lógica de negocio reside exclusivamente en el Service.

```
Cliente HTTP
    │
    ▼
┌─────────────────────────┐
│     UsuarioController   │  @RestController — capa HTTP, delega todo al Service
└──────────┬──────────────┘
           │
    ▼
┌─────────────────────────┐
│     UsuarioService      │  @Service — lógica de negocio, validaciones de dominio
│   (UsuarioServiceImpl)  │
└──────────┬──────────────┘
           │
    ▼
┌─────────────────────────┐
│   UsuarioRepository     │  @Repository — Spring Data JPA
└──────────┬──────────────┘
           │
    ▼
┌─────────────────────────┐
│     Usuario (Entity)    │  @Entity JPA — mapeo ORM
└──────────┬──────────────┘
           │
    ▼
   PostgreSQL (sdd)
```

El manejo de errores es centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`).
El mapeo entre entidad y DTOs se realiza en el Service mediante un mapper interno (sin frameworks externos).

---

## 2. Estructura de paquetes

```
com.sdd.sdd
├── usuario
│   ├── controller
│   │   └── UsuarioController.java
│   ├── service
│   │   ├── UsuarioService.java          (interface)
│   │   └── UsuarioServiceImpl.java
│   ├── repository
│   │   └── UsuarioRepository.java
│   ├── entity
│   │   └── Usuario.java
│   ├── dto
│   │   ├── UsuarioRequest.java
│   │   ├── UsuarioUpdateRequest.java
│   │   └── UsuarioResponse.java
│   └── mapper
│       └── UsuarioMapper.java
├── common
│   ├── dto
│   │   ├── ApiError.java
│   │   └── PageResponse.java
│   └── exception
│       ├── RecursoNoEncontradoException.java
│       ├── DuplicadoException.java
│       └── GlobalExceptionHandler.java
└── config
    └── SecurityConfig.java              (expone BCryptPasswordEncoder bean)
```

> **Convención**: todos los componentes del módulo viven bajo `com.sdd.sdd.usuario`. Los componentes transversales (errores, paginación) viven bajo `com.sdd.sdd.common`. La configuración mínima de seguridad en `com.sdd.sdd.config`.

---

## 3. Modelo de datos

### 3.1 Entidad `Usuario`

```java
@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
@EnableJpaAuditing  // activado en SddApplication o clase @Configuration
```

| Campo              | Tipo Java             | Columna BD              | Restricciones                        |
|--------------------|-----------------------|-------------------------|--------------------------------------|
| `id`               | `Long`                | `id BIGSERIAL`          | PK, NOT NULL, auto-incremental       |
| `nombres`          | `String`              | `nombres VARCHAR(100)`  | NOT NULL                             |
| `apellidos`        | `String`              | `apellidos VARCHAR(100)`| NOT NULL                             |
| `username`         | `String`              | `username VARCHAR(50)`  | NOT NULL, UNIQUE                     |
| `email`            | `String`              | `email VARCHAR(150)`    | NOT NULL, UNIQUE                     |
| `password`         | `String`              | `password VARCHAR(255)` | NOT NULL (hash BCrypt)               |
| `estado`           | `EstadoUsuario` (enum)| `estado VARCHAR(20)`    | NOT NULL, DEFAULT 'ACTIVO'           |
| `fechaCreacion`    | `OffsetDateTime`      | `fecha_creacion TIMESTAMPTZ` | NOT NULL, @CreatedDate          |
| `fechaModificacion`| `OffsetDateTime`      | `fecha_modificacion TIMESTAMPTZ` | NOT NULL, @LastModifiedDate |

**Enum `EstadoUsuario`**: `ACTIVO`, `INACTIVO`

### 3.2 Script de migración Flyway

Ruta: `src/main/resources/db/migration/V1__create_usuarios_table.sql`

```sql
CREATE TABLE IF NOT EXISTS usuarios (
    id               BIGSERIAL        PRIMARY KEY,
    nombres          VARCHAR(100)     NOT NULL,
    apellidos        VARCHAR(100)     NOT NULL,
    username         VARCHAR(50)      NOT NULL,
    email            VARCHAR(150)     NOT NULL,
    password         VARCHAR(255)     NOT NULL,
    estado           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion   TIMESTAMPTZ      NOT NULL,
    fecha_modificacion TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT uq_usuarios_email    UNIQUE (email),
    CONSTRAINT ck_usuarios_estado   CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios (username);
CREATE INDEX IF NOT EXISTS idx_usuarios_email    ON usuarios (email);
CREATE INDEX IF NOT EXISTS idx_usuarios_estado   ON usuarios (estado);
```

---

## 4. DTOs

### 4.1 `UsuarioRequest` (POST — creación)

| Campo      | Tipo     | Validaciones                                      |
|------------|----------|---------------------------------------------------|
| `nombres`  | `String` | `@NotBlank`, `@Size(max=100)`                     |
| `apellidos`| `String` | `@NotBlank`, `@Size(max=100)`                     |
| `username` | `String` | `@NotBlank`, `@Size(min=3, max=50)`, `@Pattern`  |
| `email`    | `String` | `@NotBlank`, `@Email`, `@Size(max=150)`           |
| `password` | `String` | `@NotBlank`, `@Size(min=8, max=100)`              |

> `@Pattern` para username: solo alfanuméricos, guiones y guiones bajos (`^[a-zA-Z0-9_-]{3,50}$`).
> El campo `password` lleva `@JsonProperty(access = WRITE_ONLY)` para evitar serialización accidental.

### 4.2 `UsuarioUpdateRequest` (PUT — edición)

| Campo      | Tipo            | Validaciones                            |
|------------|-----------------|-----------------------------------------|
| `nombres`  | `String`        | `@NotBlank`, `@Size(max=100)`           |
| `apellidos`| `String`        | `@NotBlank`, `@Size(max=100)`           |
| `email`    | `String`        | `@NotBlank`, `@Email`, `@Size(max=150)` |
| `estado`   | `EstadoUsuario` | `@NotNull`                              |

### 4.3 `UsuarioResponse` (respuestas)

| Campo              | Tipo             | Notas                        |
|--------------------|------------------|------------------------------|
| `id`               | `Long`           |                              |
| `nombres`          | `String`         |                              |
| `apellidos`        | `String`         |                              |
| `username`         | `String`         |                              |
| `email`            | `String`         |                              |
| `estado`           | `EstadoUsuario`  |                              |
| `fechaCreacion`    | `OffsetDateTime` |                              |
| `fechaModificacion`| `OffsetDateTime` |                              |

> **El campo `password` nunca aparece en este DTO.**

### 4.4 `ApiError` (errores uniformes)

```json
{
  "timestamp": "2026-08-18T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "mensaje": "El username 'jdoe' ya está registrado.",
  "detalle": "/api/usuarios"
}
```

Para errores de validación (HTTP 400), se agrega el campo `errores`:

```json
{
  "timestamp": "2026-08-18T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Validación fallida.",
  "detalle": "/api/usuarios",
  "errores": [
    { "campo": "email", "mensaje": "debe ser una dirección de correo electrónico con formato correcto" },
    { "campo": "password", "mensaje": "el tamaño debe estar entre 8 y 100" }
  ]
}
```

### 4.5 `PageResponse<T>` (listado paginado)

```json
{
  "contenido": [ ... ],
  "pagina": 0,
  "tamano": 10,
  "totalElementos": 42,
  "totalPaginas": 5,
  "ultimo": false
}
```

---

## 5. Capa de Servicio

### Interface `UsuarioService`

```java
UsuarioResponse registrar(UsuarioRequest request);
UsuarioResponse obtenerPorId(Long id);
UsuarioResponse obtenerPorUsername(String username);
PageResponse<UsuarioResponse> listar(String username, String email, EstadoUsuario estado, Pageable pageable);
UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);
void eliminarLogico(Long id);
```

### Flujo: Registrar usuario

```
1. Validar UsuarioRequest (@Valid — falla → 400)
2. Verificar username no duplicado → lanzar DuplicadoException si existe (→ 409)
3. Verificar email no duplicado   → lanzar DuplicadoException si existe (→ 409)
4. Cifrar password con BCryptPasswordEncoder
5. Construir entidad Usuario con estado=ACTIVO
6. Persistir mediante UsuarioRepository.save()
7. Mapear entidad → UsuarioResponse
8. Retornar UsuarioResponse (Controller responde 201)
```

### Flujo: Actualizar usuario

```
1. Validar UsuarioUpdateRequest (@Valid — falla → 400)
2. Buscar usuario por id → lanzar RecursoNoEncontradoException si no existe (→ 404)
3. Si el email cambió, verificar que no exista en otro usuario → DuplicadoException (→ 409)
4. Actualizar campos: nombres, apellidos, email, estado
5. fechaModificacion se actualiza automáticamente por @LastModifiedDate
6. Persistir y mapear → UsuarioResponse (Controller responde 200)
```

### Flujo: Eliminación lógica

```
1. Buscar usuario por id → lanzar RecursoNoEncontradoException si no existe (→ 404)
2. Cambiar estado a INACTIVO
3. fechaModificacion se actualiza automáticamente
4. Persistir (Controller responde 204)
```

### Flujo: Listar con filtros

```
1. Construir Specification dinámica combinando los filtros opcionales
   (username LIKE, email LIKE, estado =)
2. Llamar UsuarioRepository.findAll(spec, pageable)
3. Mapear Page<Usuario> → PageResponse<UsuarioResponse>
4. Retornar (Controller responde 200)
```

---

## 6. Capa de Repositorio

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long>,
                                            JpaSpecificationExecutor<Usuario> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    Optional<Usuario> findByUsername(String username);
}
```

Los filtros dinámicos del listado se implementan con `JpaSpecificationExecutor` y una clase `UsuarioSpecification` (inner class estática o clase separada) que construye predicados con la API Criteria.

---

## 7. Capa de Controlador

### `UsuarioController`

| Método | Path                                  | Método Service         | Respuesta       |
|--------|---------------------------------------|------------------------|-----------------|
| POST   | `/api/usuarios`                       | `registrar()`          | 201 + body      |
| GET    | `/api/usuarios`                       | `listar()`             | 200 + body      |
| GET    | `/api/usuarios/{id}`                  | `obtenerPorId()`       | 200 + body      |
| GET    | `/api/usuarios/username/{username}`   | `obtenerPorUsername()` | 200 + body      |
| PUT    | `/api/usuarios/{id}`                  | `actualizar()`         | 200 + body      |
| DELETE | `/api/usuarios/{id}`                  | `eliminarLogico()`     | 204 sin cuerpo  |

El controlador no contiene lógica de negocio. Solo:
1. Recibe y valida la request (`@Valid`).
2. Llama al servicio.
3. Construye el `ResponseEntity` con el código HTTP correcto.

---

## 8. Manejo global de excepciones

### `GlobalExceptionHandler` (`@RestControllerAdvice`)

| Excepción                          | HTTP | Descripción                       |
|------------------------------------|------|-----------------------------------|
| `RecursoNoEncontradoException`     | 404  | Usuario no encontrado             |
| `DuplicadoException`               | 409  | username o email ya existe        |
| `MethodArgumentNotValidException`  | 400  | Fallo de Bean Validation          |
| `ConstraintViolationException`     | 400  | Violación de constraint           |
| `HttpMessageNotReadableException`  | 400  | JSON malformado                   |
| `Exception` (fallback)             | 500  | Error interno no controlado       |

Todas las respuestas de error usan la estructura `ApiError`.

---

## 9. Configuración de seguridad mínima

`SecurityConfig` expone únicamente un `@Bean` de `BCryptPasswordEncoder`. No activa autenticación HTTP (fuera del alcance de este módulo).

```java
@Configuration
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> Si en el futuro se activa Spring Security completo, este bean es compatible y reutilizable.

---

## 10. JPA Auditing

Se habilita `@EnableJpaAuditing` en la clase principal `SddApplication` (o en una clase `@Configuration` dedicada). La entidad `Usuario` usa `@EntityListeners(AuditingEntityListener.class)`.

Los campos auditados usan `OffsetDateTime` para compatibilidad con zonas horarias:

```java
@CreatedDate
@Column(name = "fecha_creacion", nullable = false, updatable = false)
private OffsetDateTime fechaCreacion;

@LastModifiedDate
@Column(name = "fecha_modificacion", nullable = false)
private OffsetDateTime fechaModificacion;
```

---

## 11. Dependencias a agregar en `pom.xml`

| Dependencia                                          | Scope   | Propósito                             |
|------------------------------------------------------|---------|---------------------------------------|
| `spring-boot-starter-validation`                     | compile | Bean Validation / Hibernate Validator |
| `spring-boot-starter-security`                       | compile | BCryptPasswordEncoder                 |
| `org.flywaydb:flyway-core`                           | compile | Motor de migraciones Flyway           |
| `org.flywaydb:flyway-database-postgresql`            | compile | Driver Flyway para PostgreSQL         |

> No se agrega `spring-boot-starter-test` por separado porque `spring-boot-starter-data-jpa-test` y `spring-boot-starter-webmvc-test` ya lo cubren (Spring Boot 4.x los incluye transitivamente).

---

## 12. Cambios en `application.properties`

Se agregan propiedades Flyway. Las existentes no se modifican.

```properties
# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

---

## 13. Diagrama de flujo — Registro de usuario

```
POST /api/usuarios
        │
        ▼
  @Valid UsuarioRequest
        │
   ┌────┴────┐
   │ inválido│──→ 400 ApiError (errores de validación)
   └────┬────┘
        │ válido
        ▼
  ¿username existe?
   ┌────┴────┐
   │   sí   │──→ 409 ApiError
   └────┬────┘
        │ no
        ▼
  ¿email existe?
   ┌────┴────┐
   │   sí   │──→ 409 ApiError
   └────┬────┘
        │ no
        ▼
  BCrypt(password)
        │
        ▼
  Persistir Usuario
        │
        ▼
  201 UsuarioResponse
```

---

## 14. Árbol de archivos a crear

```
src/
├── main/
│   ├── java/com/sdd/sdd/
│   │   ├── SddApplication.java                          ← modificar: @EnableJpaAuditing
│   │   ├── config/
│   │   │   └── SecurityConfig.java                      ← nuevo
│   │   ├── common/
│   │   │   ├── dto/
│   │   │   │   ├── ApiError.java                        ← nuevo
│   │   │   │   └── PageResponse.java                    ← nuevo
│   │   │   └── exception/
│   │   │       ├── RecursoNoEncontradoException.java     ← nuevo
│   │   │       ├── DuplicadoException.java               ← nuevo
│   │   │       └── GlobalExceptionHandler.java           ← nuevo
│   │   └── usuario/
│   │       ├── controller/
│   │       │   └── UsuarioController.java                ← nuevo
│   │       ├── service/
│   │       │   ├── UsuarioService.java                   ← nuevo
│   │       │   └── UsuarioServiceImpl.java               ← nuevo
│   │       ├── repository/
│   │       │   └── UsuarioRepository.java                ← nuevo
│   │       ├── entity/
│   │       │   ├── Usuario.java                          ← nuevo
│   │       │   └── EstadoUsuario.java                    ← nuevo (enum)
│   │       ├── dto/
│   │       │   ├── UsuarioRequest.java                   ← nuevo
│   │       │   ├── UsuarioUpdateRequest.java             ← nuevo
│   │       │   └── UsuarioResponse.java                  ← nuevo
│   │       └── mapper/
│   │           └── UsuarioMapper.java                    ← nuevo
│   └── resources/
│       ├── application.properties                        ← modificar: agregar Flyway props
│       └── db/migration/
│           └── V1__create_usuarios_table.sql             ← nuevo
├── test/
│   └── java/com/sdd/sdd/
│       └── usuario/
│           ├── service/
│           │   └── UsuarioServiceImplTest.java           ← nuevo
│           ├── repository/
│           │   └── UsuarioRepositoryTest.java            ← nuevo
│           └── controller/
│               └── UsuarioControllerTest.java            ← nuevo
pom.xml                                                   ← modificar: agregar dependencias
```
