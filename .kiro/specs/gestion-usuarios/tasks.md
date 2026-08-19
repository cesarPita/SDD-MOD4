# Tasks — Gestión de Usuarios

## Implementation Plan

- [x] 1. Add required dependencies to `pom.xml`
  - Add `spring-boot-starter-validation` for Bean Validation (RF-08-05)
  - Add `spring-boot-starter-security` to access `BCryptPasswordEncoder` (RF-08-01)
  - Add `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` for schema migration management (RF-05-01)
  - **File**: `pom.xml`
  - **Acceptance**: project compiles with `BCryptPasswordEncoder`, `@NotBlank`, `@Email`, and `@Valid` all resolvable on the classpath

- [x] 2. Configure Flyway properties in `application.properties`
  - Append `spring.flyway.enabled=true`, `spring.flyway.locations=classpath:db/migration`, and `spring.flyway.baseline-on-migrate=true` without altering existing datasource properties (RF-05-01)
  - **File**: `src/main/resources/application.properties`
  - **Acceptance**: existing datasource config (`localhost:5444/sdd`) is unchanged; Flyway locates the migration directory on startup

- [x] 3. Create Flyway migration script `V1__create_usuarios_table.sql`
  - Write `CREATE TABLE IF NOT EXISTS usuarios` with all columns from the data model: `id BIGSERIAL PK`, `nombres`, `apellidos`, `username`, `email`, `password`, `estado`, `fecha_creacion TIMESTAMPTZ`, `fecha_modificacion TIMESTAMPTZ` (RF-05-02, RF-05-05, RF-05-06)
  - Add `CONSTRAINT uq_usuarios_username UNIQUE (username)` and `CONSTRAINT uq_usuarios_email UNIQUE (email)` (RF-05-03)
  - Add `CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))`
  - Add indexes on `username`, `email`, and `estado` (RF-05-04)
  - **File**: `src/main/resources/db/migration/V1__create_usuarios_table.sql`
  - **Acceptance**: script runs without errors against PostgreSQL `sdd`; table, constraints, and indexes exist; Flyway records the migration in `flyway_schema_history`

- [x] 4. Enable JPA Auditing in `SddApplication`
  - Add `@EnableJpaAuditing` to `SddApplication` so that `@CreatedDate` and `@LastModifiedDate` are populated automatically (RF-09-01, RF-09-02)
  - **File**: `src/main/java/com/sdd/sdd/SddApplication.java`
  - **Acceptance**: application context starts without auditing-related errors

- [x] 5. Create `SecurityConfig` exposing `BCryptPasswordEncoder` bean
  - Create `@Configuration` class that declares a `BCryptPasswordEncoder` `@Bean` and disables the default Spring Security HTTP login screen so REST endpoints remain open (RF-08-01, RNF-01)
  - **File**: `src/main/java/com/sdd/sdd/config/SecurityConfig.java`
  - **Acceptance**: `BCryptPasswordEncoder` is injectable; no login prompt appears; all REST endpoints are reachable without authentication

- [x] 6. Create `EstadoUsuario` enum
  - Declare enum with values `ACTIVO` and `INACTIVO` so it can be used as a JPA `@Enumerated(EnumType.STRING)` field and as a filter parameter (RF-04-02, RF-02-04)
  - **File**: `src/main/java/com/sdd/sdd/usuario/entity/EstadoUsuario.java`
  - **Acceptance**: enum compiles; usable as a JPA column type with string persistence

- [x] 7. Create `Usuario` JPA entity
  - Annotate with `@Entity`, `@Table(name = "usuarios")`, `@EntityListeners(AuditingEntityListener.class)` (RF-09-01, RF-09-02)
  - Declare fields: `id` (`Long`, `@GeneratedValue IDENTITY`), `nombres`, `apellidos`, `username`, `email`, `password`, `estado` (`EstadoUsuario`, `@Enumerated(EnumType.STRING)`), `fechaCreacion` (`OffsetDateTime`, `@CreatedDate`, `updatable=false`), `fechaModificacion` (`OffsetDateTime`, `@LastModifiedDate`)
  - Apply Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
  - Ensure no generated `toString()` includes `password` — use `@ToString(exclude = "password")` (RF-08-02)
  - **File**: `src/main/java/com/sdd/sdd/usuario/entity/Usuario.java`
  - **Acceptance**: entity maps to `usuarios` table without errors; audit fields populate automatically; `password` absent from any `toString()` output

- [x] 8. Create `UsuarioRepository`
  - Extend `JpaRepository<Usuario, Long>` and `JpaSpecificationExecutor<Usuario>` (RF-02-03, RF-02-04)
  - Declare derived query methods: `existsByUsername`, `existsByEmail`, `existsByEmailAndIdNot`, `findByUsername` (RF-01-03, RF-01-04, RF-03-04)
  - **File**: `src/main/java/com/sdd/sdd/usuario/repository/UsuarioRepository.java`
  - **Acceptance**: interface compiles; Spring Data generates all implementations on startup

- [x] 9. Create `UsuarioRequest` DTO
  - Declare fields `nombres`, `apellidos`, `username`, `email`, `password` with Bean Validation constraints: `@NotBlank`, `@Size`, `@Email`, and `@Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$")` on `username` (RF-01-02, RF-01-05, RF-08-05)
  - Mark `password` with `@JsonProperty(access = WRITE_ONLY)` to prevent accidental serialization (RF-08-03)
  - Apply Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
  - **File**: `src/main/java/com/sdd/sdd/usuario/dto/UsuarioRequest.java`
  - **Acceptance**: `password` field never appears in serialized JSON output; all validation annotations are correctly imported

- [x] 10. Create `UsuarioUpdateRequest` DTO
  - Declare only the modifiable fields: `nombres`, `apellidos`, `email`, `estado` with appropriate `@NotBlank`, `@Size`, `@Email`, and `@NotNull` constraints (RF-03-02, RF-03-03)
  - Apply Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
  - **File**: `src/main/java/com/sdd/sdd/usuario/dto/UsuarioUpdateRequest.java`
  - **Acceptance**: `username` and `password` are absent; all four modifiable fields have correct validation annotations

- [x] 11. Create `UsuarioResponse` DTO
  - Declare fields: `id`, `nombres`, `apellidos`, `username`, `email`, `estado`, `fechaCreacion`, `fechaModificacion` — no `password` field (RF-07-02, RF-08-03)
  - Use `OffsetDateTime` for date fields; apply Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
  - **File**: `src/main/java/com/sdd/sdd/usuario/dto/UsuarioResponse.java`
  - **Acceptance**: `password` is absent from the class and from any serialized JSON

- [x] 12. Create `ApiError` and `PageResponse<T>` common DTOs
  - `ApiError`: fields `timestamp` (`OffsetDateTime`), `status` (`int`), `error`, `mensaje`, `detalle` (all `String`), and nullable `errores` (`List<CampoError>`); include inner static class `CampoError` with `campo` and `mensaje` (RF-07-04, RF-07-05)
  - `PageResponse<T>`: generic class with fields `contenido` (`List<T>`), `pagina`, `tamano`, `totalElementos`, `totalPaginas`, `ultimo`; include static factory `of(Page<T> page)` that maps all Spring `Page` metadata (RF-07-03)
  - **Files**: `src/main/java/com/sdd/sdd/common/dto/ApiError.java`, `src/main/java/com/sdd/sdd/common/dto/PageResponse.java`
  - **Acceptance**: `ApiError` serializes correctly with and without `errores`; `PageResponse.of()` correctly maps all pagination metadata from a Spring `Page`

- [x] 13. Create `RecursoNoEncontradoException` and `DuplicadoException`
  - Both extend `RuntimeException` with a single `String message` constructor (RF-02-01, RF-03-06, RF-04-04, RF-01-03, RF-01-04)
  - **Files**: `src/main/java/com/sdd/sdd/common/exception/RecursoNoEncontradoException.java`, `src/main/java/com/sdd/sdd/common/exception/DuplicadoException.java`
  - **Acceptance**: both exceptions compile and are throwable from service layer methods

- [x] 14. Create `GlobalExceptionHandler`
  - Annotate with `@RestControllerAdvice`; all handler methods return `ResponseEntity<ApiError>` with `OffsetDateTime.now()` as timestamp (RF-07-04, RNF-04)
  - Handle `RecursoNoEncontradoException` → HTTP 404
  - Handle `DuplicadoException` → HTTP 409
  - Handle `MethodArgumentNotValidException` → HTTP 400 with `errores` list of `CampoError` populated from binding result (RF-07-05)
  - Handle `HttpMessageNotReadableException` → HTTP 400
  - Handle generic `Exception` → HTTP 500
  - **File**: `src/main/java/com/sdd/sdd/common/exception/GlobalExceptionHandler.java`
  - **Acceptance**: each mapped exception returns the correct HTTP status and a well-formed `ApiError` body

- [x] 15. Create `UsuarioMapper` utility class
  - Declare as a non-instantiable utility class (private constructor)
  - Implement `static UsuarioResponse toResponse(Usuario)` — must never copy `password` (RF-08-03)
  - Implement `static Usuario toEntity(UsuarioRequest, String passwordCifrado)` — receives the already-encoded password, never the raw value (RF-08-01)
  - **File**: `src/main/java/com/sdd/sdd/usuario/mapper/UsuarioMapper.java`
  - **Acceptance**: `toResponse()` output never contains `password`; `toEntity()` stores only the encoded value; all fields map correctly

- [x] 16. Create `UsuarioService` interface
  - Declare all six service contract methods: `registrar(UsuarioRequest)`, `obtenerPorId(Long)`, `obtenerPorUsername(String)`, `listar(String, String, EstadoUsuario, Pageable)`, `actualizar(Long, UsuarioUpdateRequest)`, `eliminarLogico(Long)` (RF-01 through RF-04)
  - **File**: `src/main/java/com/sdd/sdd/usuario/service/UsuarioService.java`
  - **Acceptance**: interface compiles with all correct method signatures

- [x] 17. Implement `UsuarioServiceImpl`
  - Annotate with `@Service`; inject `UsuarioRepository` and `BCryptPasswordEncoder` via constructor
  - `registrar`: check `existsByUsername` → throw `DuplicadoException` if true (RF-01-03); check `existsByEmail` → throw `DuplicadoException` if true (RF-01-04); encode password with BCrypt (RF-01-06); set `estado = ACTIVO` (RF-01-07); persist and return `UsuarioResponse`
  - `obtenerPorId`: find or throw `RecursoNoEncontradoException` (RF-02-01)
  - `obtenerPorUsername`: find or throw `RecursoNoEncontradoException` (RF-02-02)
  - `listar`: build dynamic `Specification<Usuario>` from optional `username` (LIKE), `email` (LIKE), and `estado` (=) parameters; call `findAll(spec, pageable)`; map to `PageResponse<UsuarioResponse>` (RF-02-03, RF-02-04, RF-02-05)
  - `actualizar`: find or throw `RecursoNoEncontradoException` (RF-03-06); if email changed, call `existsByEmailAndIdNot` → throw `DuplicadoException` if true (RF-03-04); update allowed fields only (RF-03-02, RF-03-03)
  - `eliminarLogico`: find or throw `RecursoNoEncontradoException` (RF-04-04); set `estado = INACTIVO`, do not delete the record (RF-04-01, RF-04-02)
  - No log statement anywhere in the class references the `password` field (RF-08-02)
  - **File**: `src/main/java/com/sdd/sdd/usuario/service/UsuarioServiceImpl.java`
  - **Acceptance**: all six methods behave per requirements; no password value ever written to logs

- [x] 18. Implement `UsuarioController`
  - Annotate with `@RestController` and `@RequestMapping("/api/usuarios")`; inject `UsuarioService` via constructor (RF-06)
  - `POST /api/usuarios`: accept `@Valid @RequestBody UsuarioRequest`; call `registrar()`; return `ResponseEntity.created(uri).body(response)` with HTTP 201 and `Location` header (RF-01-09)
  - `GET /api/usuarios`: accept optional `@RequestParam` `username`, `email`, `estado` and `Pageable`; call `listar()`; return HTTP 200 (RF-02-03, RF-02-04, RF-02-05)
  - `GET /api/usuarios/{id}`: call `obtenerPorId()`; return HTTP 200 (RF-02-01)
  - `GET /api/usuarios/username/{username}`: call `obtenerPorUsername()`; return HTTP 200 (RF-02-02)
  - `PUT /api/usuarios/{id}`: accept `@Valid @RequestBody UsuarioUpdateRequest`; call `actualizar()`; return HTTP 200 (RF-03-01, RF-03-07)
  - `DELETE /api/usuarios/{id}`: call `eliminarLogico()`; return `ResponseEntity<Void>` with HTTP 204 (RF-04-05)
  - No business logic in the controller — delegate entirely to the service (RNF-03)
  - **File**: `src/main/java/com/sdd/sdd/usuario/controller/UsuarioController.java`
  - **Acceptance**: all six endpoints return the correct HTTP status codes; `POST` includes `Location` header; controller contains no conditional business logic

- [x] 19. Write unit tests for `UsuarioServiceImpl`
  - Use `@ExtendWith(MockitoExtension.class)`; mock `UsuarioRepository` and `BCryptPasswordEncoder`
  - Cover all 12 scenarios mapped to requirements:
    - `registrar_exitoso` → verifies `encode()` called, returns response without password (T-01, RF-01-06, RF-08-03)
    - `registrar_username_duplicado` → asserts `DuplicadoException` thrown (T-02, RF-01-03)
    - `registrar_email_duplicado` → asserts `DuplicadoException` thrown (T-03, RF-01-04)
    - `obtenerPorId_existente` → returns `UsuarioResponse` (T-06, RF-02-01)
    - `obtenerPorId_inexistente` → asserts `RecursoNoEncontradoException` thrown (T-07, RF-02-01)
    - `obtenerPorUsername_existente` → returns `UsuarioResponse` (T-08, RF-02-02)
    - `obtenerPorUsername_inexistente` → asserts `RecursoNoEncontradoException` thrown (T-09, RF-02-02)
    - `actualizar_exitoso` → updates fields, returns updated response (T-10, RF-03-01)
    - `actualizar_inexistente` → asserts `RecursoNoEncontradoException` thrown (T-11, RF-03-06)
    - `actualizar_email_duplicado` → asserts `DuplicadoException` thrown (T-12, RF-03-04)
    - `eliminarLogico_existente` → estado becomes `INACTIVO`, record not deleted (T-13, RF-04-01, RF-04-02)
    - `eliminarLogico_inexistente` → asserts `RecursoNoEncontradoException` thrown (T-14, RF-04-04)
  - **File**: `src/test/java/com/sdd/sdd/usuario/service/UsuarioServiceImplTest.java`
  - **Acceptance**: all 12 tests pass; `encode()` invocation verified in `registrar_exitoso`; no test references raw password in assertions

- [x] 20. Write repository tests for `UsuarioRepository`
  - Use `@DataJpaTest`; if H2 is incompatible with PostgreSQL dialect, document the requirement for Testcontainers or a dedicated test database
  - Cover 6 scenarios:
    - `existsByUsername_retorna_true` (RF-01-03)
    - `existsByUsername_retorna_false` (RF-01-03)
    - `existsByEmail_retorna_true` (RF-01-04)
    - `existsByEmailAndIdNot` returns `true` for same email on different record (RF-03-04)
    - `findByUsername_existente` returns non-empty `Optional` (RF-02-02)
    - `findByUsername_inexistente` returns empty `Optional` (RF-02-02)
  - Each test creates its own data and leaves no side effects
  - **File**: `src/test/java/com/sdd/sdd/usuario/repository/UsuarioRepositoryTest.java`
  - **Acceptance**: all 6 tests pass; test data is isolated per test method

- [x] 21. Write integration tests for `UsuarioController`
  - Use `@WebMvcTest(UsuarioController.class)`; mock `UsuarioService` with `@MockBean`
  - Cover all 16 scenarios mapped to requirements:
    - `post_registro_exitoso` → HTTP 201, response body contains no `password` field (T-01, RF-01-09, RF-08-03)
    - `post_username_duplicado` → HTTP 409, `ApiError` body (T-02, RF-01-03)
    - `post_email_duplicado` → HTTP 409, `ApiError` body (T-03, RF-01-04)
    - `post_datos_invalidos` → HTTP 400, `ApiError` with `errores` list (T-04, RF-07-05)
    - `post_campos_obligatorios_vacios` → HTTP 400, `ApiError` (T-05, RF-01-02)
    - `get_por_id_existente` → HTTP 200, `UsuarioResponse` body (T-06, RF-02-01)
    - `get_por_id_inexistente` → HTTP 404, `ApiError` (T-07, RF-02-01)
    - `get_por_username_existente` → HTTP 200, `UsuarioResponse` body (T-08, RF-02-02)
    - `get_por_username_inexistente` → HTTP 404, `ApiError` (T-09, RF-02-02)
    - `get_listado_paginado` → HTTP 200, `PageResponse` with pagination metadata (T-15, RF-02-03, RF-02-05)
    - `get_listado_filtro_estado` → HTTP 200, only matching records returned (T-16, RF-02-04)
    - `put_actualizar_exitoso` → HTTP 200, updated `UsuarioResponse` (T-10, RF-03-07)
    - `put_actualizar_inexistente` → HTTP 404, `ApiError` (T-11, RF-03-06)
    - `put_email_duplicado` → HTTP 409, `ApiError` (T-12, RF-03-04)
    - `delete_eliminacion_logica` → HTTP 204, empty body (T-13, RF-04-05)
    - `delete_inexistente` → HTTP 404, `ApiError` (T-14, RF-04-04)
  - Assert `password` is absent from every response body (RF-08-03)
  - **File**: `src/test/java/com/sdd/sdd/usuario/controller/UsuarioControllerTest.java`
  - **Acceptance**: all 16 tests pass; no response body contains `password`; all HTTP status codes match requirements exactly
