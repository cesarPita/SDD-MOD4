# Plan de implementación: Registro y Consulta de Personas SDD

## Overview

Implementación completa del módulo `persona` en el sistema SDD. El módulo expone cuatro
endpoints REST (POST crear, GET por ID, GET por documento, GET listar con filtros paginados)
sin modificación ni eliminación en v1. Reutiliza sin modificar toda la infraestructura
compartida: `GlobalExceptionHandler`, `PageResponse<T>`, `JpaAuditingConfig`, `SecurityConfig`
y `logback-spring.xml`.

---

## Tasks

- [x] 1. Crear las enumeraciones del módulo Persona
  - [x] 1.1 Crear `TipoDocumento`, `Genero` y `EstadoCivil` en `com.sdd.sdd.persona.entity`
    - Crear `TipoDocumento` con valores `CI`, `PASAPORTE`, `CEX`, `NIT`
    - Crear `Genero` con valores `MASCULINO`, `FEMENINO`, `OTRO`
    - Crear `EstadoCivil` con valores `SOLTERO`, `CASADO`, `DIVORCIADO`, `VIUDO`, `UNION_LIBRE`
    - _Requerimientos: 5.3_

- [x] 2. Crear la entidad JPA `Persona`
  - [x] 2.1 Crear la clase `Persona` en `com.sdd.sdd.persona.entity`
    - Anotaciones: `@Entity`, `@Table(name = "personas")`, `@EntityListeners(AuditingEntityListener.class)`, Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
    - Campos: `id` (BIGSERIAL PK), `tipoDocumento` (`@Enumerated(STRING)`), `numeroDocumento`, `complemento` (nullable), `fechaNacimiento` (`LocalDate`), `apellidoPaterno` (nullable), `apellidoMaterno` (nullable), `apellidoEsposo` (nullable), `nombres`, `genero` (`@Enumerated(STRING)`), `estadoCivil` (`@Enumerated(STRING)`)
    - Campos de auditoría: `fechaCreacion` (`OffsetDateTime`, `@CreatedDate`, `updatable = false`) y `fechaModificacion` (`OffsetDateTime`, `@LastModifiedDate`)
    - _Requerimientos: 5.1, 5.5_

- [x] 3. Crear la migración Flyway `V2__create_personas_table.sql`
  - [x] 3.1 Crear el archivo `src/main/resources/db/migration/V2__create_personas_table.sql`
    - Tabla `personas` con todos los campos definidos en el diseño
    - Constraint de unicidad `uq_personas_documento` usando `UNIQUE (tipo_documento, numero_documento, COALESCE(complemento, ''))` para que NULL participe en la unicidad
    - Constraints CHECK para `tipo_documento`, `genero` y `estado_civil`
    - Índices sobre `tipo_documento`, `numero_documento` y `(tipo_documento, numero_documento, COALESCE(complemento, ''))` para optimizar consultas
    - _Requerimientos: 5.1, 5.2, 5.3, 5.4_

- [x] 4. Crear `PersonaRepository`
  - [x] 4.1 Crear la interfaz `PersonaRepository` en `com.sdd.sdd.persona.repository`
    - Extender `JpaRepository<Persona, Long>` y `JpaSpecificationExecutor<Persona>`
    - Declarar `boolean existsByTipoDocumentoAndNumeroDocumentoAndComplemento(TipoDocumento, String, String)`
    - Declarar `Optional<Persona> findByTipoDocumentoAndNumeroDocumentoAndComplemento(TipoDocumento, String, String)`
    - _Requerimientos: 1.3, 1.4, 3.1, 3.3, 4.7_

- [x] 5. Crear los DTOs `PersonaRequest` y `PersonaResponse`
  - [x] 5.1 Crear `PersonaRequest` en `com.sdd.sdd.persona.dto`
    - Campos con Bean Validation: `tipoDocumento` (`@NotNull`), `numeroDocumento` (`@NotBlank @Size(max=50)`), `complemento` (`@Size(max=10)`, opcional), `fechaNacimiento` (`@NotNull @Past`), `apellidoPaterno` (opcional), `apellidoMaterno` (opcional), `apellidoEsposo` (opcional), `nombres` (`@NotBlank @Size(max=200)`), `genero` (`@NotNull`), `estadoCivil` (`@NotNull`)
    - Todos los campos documentados con `@Schema(description, example)` siguiendo el patrón de `UsuarioRequest`
    - _Requerimientos: 6.2, 6.4_
  - [x] 5.2 Crear `PersonaResponse` en `com.sdd.sdd.persona.dto`
    - Incluye todos los campos de `PersonaRequest` más `id` (`Long`), `fechaCreacion` (`OffsetDateTime`) y `fechaModificacion` (`OffsetDateTime`)
    - Todos los campos documentados con `@Schema`
    - No hay campos de solo escritura
    - _Requerimientos: 1.1, 2.1, 6.4_

- [x] 6. Crear `PersonaMapper`
  - [x] 6.1 Crear la clase utilitaria estática `PersonaMapper` en `com.sdd.sdd.persona.mapper`
    - Constructor privado que lanza `UnsupportedOperationException("Utility class")`
    - `public static PersonaResponse toResponse(Persona persona)` — mapea todos los campos incluyendo auditoría
    - `public static Persona toEntity(PersonaRequest request)` — no asigna `id`, `fechaCreacion` ni `fechaModificacion` (gestionados por JPA Auditing)
    - Seguir el patrón de `UsuarioMapper`
    - _Requerimientos: 1.1, 1.2_

- [x] 7. Crear `PersonaService` y `PersonaServiceImpl`
  - [x] 7.1 Crear la interfaz `PersonaService` en `com.sdd.sdd.persona.service`
    - Declarar: `PersonaResponse crear(PersonaRequest)`, `PersonaResponse obtenerPorId(Long)`, `PersonaResponse obtenerPorDocumento(TipoDocumento, String, String)`, `PageResponse<PersonaResponse> listar(String, String, TipoDocumento, Pageable)`
    - _Requerimientos: 1.1, 2.1, 3.1, 4.1_
  - [x] 7.2 Crear `PersonaServiceImpl` en `com.sdd.sdd.persona.service`
    - Anotaciones: `@Service`, `@Transactional`
    - Logger: `private static final Logger log = LoggerFactory.getLogger(PersonaServiceImpl.class)`
    - `crear`: verificar unicidad con `existsByTipoDocumentoAndNumeroDocumentoAndComplemento`; si existe → `DuplicadoException` con mensaje descriptivo + log WARN; si no → `PersonaMapper.toEntity` → `repository.save` → `PersonaMapper.toResponse` + log INFO con id generado
    - `obtenerPorId`: `@Transactional(readOnly=true)`, `findById.orElseThrow(RecursoNoEncontradoException)` + log WARN si no encontrado
    - `obtenerPorDocumento`: `@Transactional(readOnly=true)`, `findByTipoDocumentoAndNumeroDocumentoAndComplemento` (complemento llega como `null` cuando no se provee) + log WARN si no encontrado
    - `listar`: `@Transactional(readOnly=true)`, construir `Specification<Persona>` con lambdas inline para filtros LIKE insensibles a mayúsculas (nombres, apellidoPaterno) y exacto (tipoDocumento); `repository.findAll(spec, pageable)` → `PageResponse.of(...map(PersonaMapper::toResponse))`
    - _Requerimientos: 1.1, 1.3, 1.4, 1.6, 2.2, 2.3, 3.1, 3.2, 3.3, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 7.2, 7.3, 7.4_

- [x] 8. Crear `PersonaController`
  - [x] 8.1 Crear `PersonaController` en `com.sdd.sdd.persona.controller`
    - `@Tag(name = "Personas", description = "Registro y consulta de personas naturales")`
    - `@RestController @RequestMapping("/api/personas")`
    - Logger SLF4J declarado como campo estático privado final
    - `POST /api/personas` → `crear(@Valid @RequestBody PersonaRequest)` → 201 + header `Location: /api/personas/{id}` + cuerpo `PersonaResponse`
    - `GET /api/personas/{id}` → `obtenerPorId(@PathVariable Long)` → 200 / 404
    - `GET /api/personas/documento` → `obtenerPorDocumento(@RequestParam TipoDocumento, @RequestParam String, @RequestParam(required=false) String)` → 200 / 400 / 404
    - `GET /api/personas` → `listar(@RequestParam(required=false) filtros..., @Parameter(hidden=true) Pageable)` → 200
    - Cada endpoint anotado con `@Operation`, `@ApiResponses` (incluyendo 400, 404, 409 donde aplica, 500), `@Parameter` para path/query vars
    - Logs INFO de inicio y fin de cada operación (sin datos sensibles)
    - Delegar 100% de la lógica al service; no acceder a repository directamente
    - _Requerimientos: 1.1, 2.1, 3.1, 3.4, 4.1, 6.1, 6.5, 7.2_

- [ ] 9. Checkpoint — compilar el módulo
  - Ejecutar `mvn compile` y verificar que no hay errores de compilación. Resolver cualquier problema antes de continuar con los tests.

- [ ] 10. Implementar `PersonaServiceImplTest`
  - [ ] 10.1 Crear `PersonaServiceImplTest` en `src/test/java/com/sdd/sdd/persona/service`
    - `@ExtendWith(MockitoExtension.class)`, `@Mock PersonaRepository`, `@InjectMocks PersonaServiceImpl`
    - **crear — camino feliz:** mock `existsByTipoDocumento…` → `false`, `save` → entidad con id; verificar response.getId(), `verify(repository, times(1)).save(any())`
    - **crear — duplicado con complemento no nulo:** mock `existsByTipoDocumento…` → `true`; verificar `DuplicadoException` + `verify(repository, never()).save(any())`
    - **crear — duplicado con complemento null (Property 1):** ídem con complemento `null`; verificar `DuplicadoException`
    - **obtenerPorId — existente:** mock `findById` → `Optional.of(entidad)`; verificar response
    - **obtenerPorId — inexistente:** mock `findById` → `Optional.empty()`; verificar `RecursoNoEncontradoException`
    - **obtenerPorDocumento — existente:** mock `findByTipoDocumento…` → `Optional.of(entidad)`; verificar response
    - **obtenerPorDocumento — inexistente:** mock `findByTipoDocumento…` → `Optional.empty()`; verificar `RecursoNoEncontradoException`
    - **obtenerPorDocumento — complemento ausente equivale a null (Property 2):** llamar con `complemento=null`; mock devuelve entidad; verificar que se pasa `null` al repository
    - **listar — sin filtros:** mock `findAll(spec, pageable)` → página con resultados; verificar contenido
    - **listar — página vacía:** mock devuelve página vacía; verificar `totalElementos=0`
    - **listar — filtro por nombres (Property 3):** verificar que la Specification construida no es nula cuando se provee el filtro
    - **listar — filtro por apellidoPaterno (Property 4):** ídem
    - **listar — filtro por tipoDocumento (Property 5):** ídem
    - _Requerimientos: 1.3, 1.4, 2.2, 3.2, 3.3, 4.2, 4.3, 4.4, 4.6_
  - [ ]* 10.2 Escribir pruebas unitarias adicionales para cobertura de casos límite en `PersonaServiceImplTest`
    - Verificar mensajes de excepción en `DuplicadoException` y `RecursoNoEncontradoException`
    - Verificar que `obtenerPorId` y `obtenerPorDocumento` usan `@Transactional(readOnly=true)` validando el comportamiento del proxy (opcional si ya cubierto)
    - _Requerimientos: 1.3, 2.2, 3.2_

- [ ] 11. Implementar `PersonaControllerTest`
  - [ ] 11.1 Crear `PersonaControllerTest` en `src/test/java/com/sdd/sdd/persona/controller`
    - `@WebMvcTest(PersonaController.class)`, `@Import(SecurityConfig.class)`
    - `@TestConfiguration` con `@Bean ObjectMapper` usando `new ObjectMapper().findAndRegisterModules()`
    - `@Autowired MockMvc`, `@Autowired ObjectMapper`, `@MockitoBean PersonaService`
    - **POST 201:** mock `personaService.crear(any())` → response con id; verificar `status().isCreated()`, `header().exists("Location")`, `jsonPath("$.id")`, `jsonPath("$.nombres")`
    - **POST 400 — nombres blank:** request sin nombres; verificar `status().isBadRequest()`, `jsonPath("$.errores").isArray()`
    - **POST 400 — tipoDocumento null:** request sin tipoDocumento; verificar 400
    - **POST 400 — fechaNacimiento futura:** request con fecha futura; verificar 400
    - **POST 409 — identidad duplicada:** mock lanza `DuplicadoException`; verificar `status().isConflict()`
    - **GET /{id} 200:** mock `obtenerPorId(1L)` → response; verificar 200 + body
    - **GET /{id} 404:** mock lanza `RecursoNoEncontradoException`; verificar 404
    - **GET /documento 200:** mock `obtenerPorDocumento(CI, "12345678", null)` → response; verificar 200
    - **GET /documento 404:** mock lanza `RecursoNoEncontradoException`; verificar 404
    - **GET /documento 400 — tipoDocumento ausente:** llamar sin `tipoDocumento`; verificar 400
    - **GET /documento 400 — numeroDocumento ausente:** llamar sin `numeroDocumento`; verificar 400
    - **GET / 200 con resultados:** mock `listar(…)` → `PageResponse` con elementos; verificar 200 + `jsonPath("$.contenido").isArray()`
    - **GET / 200 lista vacía:** mock devuelve `PageResponse` vacío; verificar 200 + `jsonPath("$.totalElementos").value(0)`
    - Construir JSON de request usando `objectMapper.writeValueAsString(Map.of(...))` para garantizar serialización correcta de enums
    - _Requerimientos: 1.1, 1.5, 2.1, 2.2, 3.1, 3.2, 3.4, 4.1, 4.6, 6.3_
  - [ ]* 11.2 Escribir pruebas adicionales de validación en `PersonaControllerTest`
    - **POST 400 — numeroDocumento blank**
    - **POST 400 — complemento excede 10 caracteres**
    - **POST 400 — estadoCivil null**
    - **POST 400 — genero null**
    - _Requerimientos: 6.2, 6.3_

- [ ] 12. Implementar `PersonaRepositoryTest`
  - [ ] 12.1 Crear `PersonaRepositoryTest` en `src/test/java/com/sdd/sdd/persona/repository`
    - Seguir exactamente el patrón de `UsuarioRepositoryTest`: `@DataJpaTest(excludeAutoConfiguration = JpaAuditingConfig.class)`, `@AutoConfigureTestDatabase(replace = Replace.NONE)`, `@TestPropertySource(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})`, `@Import(PersonaRepositoryTest.NoOpAuditingConfig.class)`
    - `@TestConfiguration static class NoOpAuditingConfig` con bean `jpaAuditingHandler` que mockea `markCreated` y `markModified`
    - Helper `nuevaPersona(TipoDocumento, String, String)` que asigna `fechaCreacion` y `fechaModificacion` manualmente con `OffsetDateTime.now()`
    - **existsByDocumento — retorna true cuando la combinación existe**
    - **existsByDocumento — retorna false cuando no existe**
    - **existsByDocumento — retorna true cuando complemento es null y ya existe (Property 7/1.4)**
    - **findByDocumento — existente retorna Optional con valor**
    - **findByDocumento — inexistente retorna Optional vacío**
    - **findByDocumento — con complemento null encuentra registro sin complemento (Property 2)**
    - **unicidad documental — rechaza duplicado con mismo complemento no nulo (Property 7)**
    - **unicidad documental — rechaza duplicado con complemento null en ambos (Property 7/1.4)**
    - Infraestructura requerida: PostgreSQL disponible en `localhost:5444/sdd` con credenciales de `application.properties`
    - _Requerimientos: 1.3, 1.4, 3.3, 5.2_
  - [ ]* 12.2 Escribir pruebas adicionales de índices y constraints CHECK en `PersonaRepositoryTest`
    - Verificar que se puede persistir una persona válida con todos los campos
    - Verificar que dos personas con el mismo tipo/número pero diferente complemento no son duplicadas
    - _Requerimientos: 5.3, 5.4_

- [ ] 13. Checkpoint final — ejecutar todas las pruebas
  - Ejecutar `mvn test` y verificar `BUILD SUCCESS`.
  - Si hay fallos, leer el error completo, identificar si el problema está en el código de producción o en la prueba, corregir y volver a ejecutar.
  - Reportar: clases de prueba ejecutadas, total de pruebas, fallos.

---

## Notes

- Las sub-tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido.
- Las tareas 1–8 deben completarse en el orden indicado por las dependencias: enums → entidad → migración / repository / DTOs → mapper → service → controller.
- Las tareas 10, 11 y 12 son independientes entre sí y pueden ejecutarse en paralelo una vez completadas las tareas de producción correspondientes.
- La constraint de unicidad documental usa `COALESCE(complemento, '')`: dos registros con el mismo `(tipo_documento, numero_documento)` y `complemento = NULL` en ambos son rechazados (Requerimientos 1.4, 5.2).
- Los métodos derivados de Spring Data pasan `null` como `IS NULL` en JPQL, lo que garantiza que `obtenerPorDocumento` con `complemento=null` resuelve correctamente a `WHERE complemento IS NULL`.
- No introducir nuevas dependencias: el proyecto ya cuenta con JPA, Bean Validation, Spring MVC, Springdoc, Flyway, PostgreSQL, Mockito y AssertJ.
- No modificar: `GlobalExceptionHandler`, `DuplicadoException`, `RecursoNoEncontradoException`, `PageResponse`, `JpaAuditingConfig`, `OpenApiConfig`, `SecurityConfig`.
- Las siete Correctness Properties del diseño se cubren con tests unitarios de servicio (Properties 1–6) y tests de repositorio (Properties 2, 7) sin biblioteca PBT externa, según la estrategia acordada en el diseño.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["3.1", "4.1", "5.1", "5.2"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["7.1"] },
    { "id": 5, "tasks": ["7.2"] },
    { "id": 6, "tasks": ["8.1"] },
    { "id": 7, "tasks": ["10.1", "11.1", "12.1"] },
    { "id": 8, "tasks": ["10.2", "11.2", "12.2"] }
  ]
}
```
