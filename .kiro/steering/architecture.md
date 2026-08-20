---
inclusion: always
---

# Arquitectura y Estandares de Desarrollo — Spring Boot SDD

Este archivo define la arquitectura obligatoria y los estandares de desarrollo para todo codigo
nuevo o modificado en este proyecto. Kiro debe aplicarlos automaticamente en cada interaccion.

---

## 1. Arquitectura en capas

La aplicacion utiliza una arquitectura por capas con separacion clara de responsabilidades:

```
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

### Responsabilidades por capa

| Capa       | Responsabilidad |
|------------|----------------|
| Controller | Exponer endpoints REST. Recibir requests. Validacion inicial con `@Valid`. Construir `ResponseEntity`. Delegar logica al Service. |
| Service    | Logica de negocio. Reglas funcionales. Coordinacion de operaciones. Manejo de transacciones. Lanzamiento de excepciones de dominio. |
| Repository | Acceso a datos exclusivamente. Consultas Spring Data JPA o `@Query`. |
| Entity     | Modelo de persistencia JPA. Sin logica de presentacion. |
| DTO        | Representacion de datos de entrada (Request) y salida (Response) de la API. |
| Mapper     | Transformacion entre Entity y DTO. |

### Reglas de dependencia entre capas — OBLIGATORIAS

- Controllers NO acceden directamente a Repositories.
- Services NO dependen de Controllers.
- No colocar logica de negocio compleja en Controllers.
- Los Controllers unicamente orquestan la llamada al Service y construyen la respuesta HTTP.

---

## 2. Organizacion de paquetes

Estructura actual del proyecto (respetar y extender coherentemente):

```
com.sdd.sdd
├── common
│   ├── audit        (AuditContext — preparado para JWT)
│   ├── dto          (ApiError, PageResponse)
│   ├── exception    (GlobalExceptionHandler, RecursoNoEncontradoException, DuplicadoException)
│   └── logging      (CorrelationIdFilter, HttpLoggingFilter)
├── config           (JpaAuditingConfig, OpenApiConfig, SecurityConfig)
└── [modulo]
    ├── controller
    ├── dto
    ├── entity
    ├── mapper
    ├── repository
    └── service
```

### Convenciones de nombrado

| Tipo       | Patron de nombre                          | Ejemplo |
|------------|-------------------------------------------|---------|
| Controller | `{Entidad}Controller`                     | `UsuarioController` |
| Service    | `{Entidad}Service` (interfaz)             | `UsuarioService` |
| Service    | `{Entidad}ServiceImpl` (implementacion)   | `UsuarioServiceImpl` |
| Repository | `{Entidad}Repository`                     | `UsuarioRepository` |
| Entity     | `{Entidad}` (singular, PascalCase)        | `Usuario` |
| DTO entrada| `{Entidad}Request`                        | `UsuarioRequest` |
| DTO salida | `{Entidad}Response`                       | `UsuarioResponse` |
| DTO update | `{Entidad}UpdateRequest`                  | `UsuarioUpdateRequest` |
| Mapper     | `{Entidad}Mapper`                         | `UsuarioMapper` |

No crear paquetes innecesarios. Cada nuevo modulo debe seguir la misma estructura.

---

## 3. DTOs

- Las APIs REST usan DTOs para requests y responses. Nunca exponer entidades JPA directamente.
- Separar `Request` y `Response` cuando los datos de entrada y salida difieran.
- Los DTOs contienen unicamente los datos necesarios para la operacion.
- Anotar con `@Schema` (OpenAPI) todos los campos de DTOs publicos.
- Campos sensibles (password, tokens) usar `@JsonProperty(access = WRITE_ONLY)` y `@Schema(accessMode = WRITE_ONLY)`.

---

## 4. Entidades JPA

Usar las anotaciones correctas:

```java
@Entity
@Table(name = "nombre_tabla")
@EntityListeners(AuditingEntityListener.class)
public class MiEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campo", nullable = false)
    private String campo;
}
```

- Usar `GenerationType.IDENTITY` para PostgreSQL.
- Definir `nullable`, `unique`, `length` en `@Column` para reflejar las constraints de la BD.
- Para fechas de auditoria usar `@CreatedDate` / `@LastModifiedDate` con tipo `OffsetDateTime`.
- El `DateTimeProvider` ya esta configurado en `JpaAuditingConfig` para devolver `OffsetDateTime`.
- No colocar logica de presentacion ni de negocio dentro de las entidades.
- Evitar relaciones EAGER innecesarias que generen N+1 queries.

---

## 5. Services

- Implementar siempre una interfaz (`UsuarioService`) y su implementacion (`UsuarioServiceImpl`).
- Anotar la implementacion con `@Service`.
- Anotar la clase con `@Transactional` solo cuando todas las operaciones del servicio lo requieran.
- Anotar metodos de solo lectura con `@Transactional(readOnly = true)`.
- Lanzar excepciones de dominio existentes:
  - `RecursoNoEncontradoException` → 404
  - `DuplicadoException` → 409
- No crear nuevas excepciones sin justificacion.
- No devolver entidades JPA directamente — siempre mapear a DTO mediante el Mapper.

---

## 6. Repositories

- Extender `JpaRepository<Entidad, Long>` como base.
- Agregar `JpaSpecificationExecutor<Entidad>` cuando se requieran filtros dinamicos.
- Usar metodos derivados de Spring Data cuando sean suficientes.
- Usar `@Query` unicamente para consultas que no puedan expresarse con metodos derivados.
- No colocar logica de negocio en Repositories.
- No agregar logging innecesario.

---

## 7. Manejo de excepciones

- El `GlobalExceptionHandler` existente en `common/exception` es el unico manejador global.
- No crear `@RestControllerAdvice` adicionales.
- No usar bloques `try/catch` para logica de negocio — lanzar la excepcion apropiada.
- El stack trace solo se registra en servidor con `log.error("mensaje", ex)`, nunca se expone al cliente.
- Estructura de error estandar: usar `ApiError` de `common/dto`.

Mapeo de excepciones a HTTP ya configurado:

| Excepcion                        | HTTP |
|----------------------------------|------|
| `RecursoNoEncontradoException`   | 404  |
| `DuplicadoException`             | 409  |
| `MethodArgumentNotValidException`| 400  |
| `HttpMessageNotReadableException`| 400  |
| `Exception` (generica)           | 500  |

---

## 8. Validacion

- Usar Bean Validation en DTOs de entrada con `@Valid` en el Controller.
- Anotaciones disponibles: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@Min`, `@Max`, `@Past`, `@Future`.
- Las reglas criticas de negocio (unicidad, estado, dependencias) se validan en el Service.
- No depender de validaciones del frontend como unica capa de validacion.

---

## 9. Respuestas REST

Usar siempre `ResponseEntity` con el codigo HTTP correcto:

| Operacion               | Codigo | Notas |
|-------------------------|--------|-------|
| POST crear              | 201    | + header `Location` apuntando al recurso creado |
| GET consultar           | 200    | |
| PUT actualizar          | 200    | |
| DELETE logico           | 204    | Sin cuerpo |
| No encontrado           | 404    | Cuerpo: `ApiError` |
| Duplicado / conflicto   | 409    | Cuerpo: `ApiError` |
| Validacion fallida      | 400    | Cuerpo: `ApiError` con lista `errores` |
| Error interno           | 500    | Cuerpo: `ApiError` sin detalle interno |

Reutilizar `PageResponse<T>` para respuestas paginadas.

---

## 10. Principios de diseno

Aplicar en todo codigo nuevo:

- **SOLID**: cada clase tiene una sola responsabilidad; depender de abstracciones.
- **DRY**: no duplicar logica — extraer a metodos o clases comunes.
- **KISS**: la solucion mas simple que funcione correctamente.
- **Separacion de responsabilidades**: cada capa hace solo lo que le corresponde.
- **Bajo acoplamiento / alta cohesion**: modulos independientes con responsabilidades claras.

Evitar sobreingenieria. No crear abstracciones que no aporten valor real al proyecto.

---

## 11. Dependencias

Antes de agregar cualquier dependencia al `pom.xml`:

1. Verificar si ya existe una dependencia equivalente en el proyecto.
2. Verificar si Spring Boot ya provee la funcionalidad via auto-configuracion.
3. Verificar compatibilidad con **Spring Boot 4.1.0** y **Java 25 (GraalVM)**.
4. No declarar version manualmente si Spring Boot BOM la gestiona.
5. Justificar la dependencia en un comentario dentro del `pom.xml`.

Dependencias actuales del proyecto:

| Dependencia | Proposito |
|-------------|-----------|
| `spring-boot-starter-webmvc` | API REST + MVC |
| `spring-boot-starter-data-jpa` | Persistencia JPA / Hibernate |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-security` | Spring Security |
| `jackson-databind` | Serializacion JSON (scope compile — requerido en runtime en Boot 4.x) |
| `flyway-core` + `flyway-database-postgresql` | Migraciones de BD |
| `postgresql` | Driver JDBC (scope runtime) |
| `lombok` | Reduccion de boilerplate |
| `springdoc-openapi-starter-webmvc-ui:3.1.0` | Swagger UI / OpenAPI 3.1 |

---

## 12. Logging

Todo el logging debe seguir las reglas del archivo `spring-boot-standards.md`.

Resumen ejecutivo:

- **PROHIBIDO**: `System.out.println()`, `System.err.println()`, `e.printStackTrace()`.
- **Obligatorio**: SLF4J con `LoggerFactory.getLogger(MiClase.class)`.
- **Placeholders**: `log.info("id={}", id)` — nunca concatenacion.
- **Nunca registrar**: passwords, tokens JWT, headers Authorization, secretos.
- El `CorrelationIdFilter` ya propaga el `correlationId` via MDC — no duplicar ese log.
- El `HttpLoggingFilter` ya registra metodo, URI, status y duracion — no duplicar.

---

## 13. Documentacion OpenAPI

Todo endpoint REST nuevo o modificado debe documentarse. Ver reglas completas en `spring-boot-standards.md`.

Anotaciones minimas requeridas por Controller:

```java
@Tag(name = "Modulo", description = "Descripcion del modulo")
@Operation(summary = "...", description = "...")
@ApiResponses({ @ApiResponse(...), ... })
@Parameter(description = "...", example = "...")
```

---

## 14. Regla principal — obligatoria

Antes de implementar cualquier funcionalidad nueva:

1. **Analizar** la arquitectura y los componentes existentes.
2. **Identificar** clases, excepciones, mappers y configuraciones reutilizables.
3. **Seguir** esta arquitectura y los estandares definidos en este archivo.
4. **No introducir** una arquitectura diferente sin justificacion tecnica explicita.
5. **No modificar** codigo no relacionado con el requerimiento actual.

Estas reglas se aplican automaticamente a todo codigo nuevo o modificado en el proyecto.