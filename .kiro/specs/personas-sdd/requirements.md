# Requirements Document

## Introduction

Este documento especifica los requerimientos funcionales del módulo **Persona** dentro del sistema SDD (Spring Boot 4.1.0 / Java 25 / PostgreSQL). El módulo permite registrar personas con sus datos de identificación documental y datos personales, consultarlas individualmente por ID o por combinación de documento, y listarlas de forma paginada con filtros opcionales.

La versión 1 no incluye operaciones de modificación ni eliminación. La identidad documental de una persona está determinada por la combinación única de (tipo_documento, numero_documento, complemento), donde el complemento es un campo opcional que participa en la constraint de unicidad incluso cuando es NULL.

---

## Glossary

- **Sistema**: La aplicación Spring Boot SDD que expone la API REST del módulo Persona.
- **Persona**: Entidad que representa a una persona natural registrada en el sistema con sus datos de identificación y datos personales.
- **TipoDocumento**: Enumeración que clasifica el documento de identidad: `CI` (Cédula de Identidad), `PASAPORTE`, `CEX` (Carné de Extranjería) o `NIT` (Número de Identificación Tributaria).
- **Complemento**: Sufijo alfanumérico opcional que, junto con el tipo y número de documento, forma la identidad documental completa. Participa en la constraint de unicidad incluso cuando es NULL.
- **Identidad documental**: La combinación (tipo_documento, numero_documento, complemento) que identifica unívocamente a una persona en el sistema.
- **Genero**: Enumeración con los valores `MASCULINO`, `FEMENINO` y `OTRO`.
- **EstadoCivil**: Enumeración con los valores `SOLTERO`, `CASADO`, `DIVORCIADO`, `VIUDO` y `UNION_LIBRE`.
- **PersonaRequest**: DTO de entrada con los datos requeridos para registrar una nueva persona.
- **PersonaResponse**: DTO de salida con los datos de una persona devueltos por la API.
- **PageResponse**: DTO genérico de paginación reutilizado del módulo `common`.
- **GlobalExceptionHandler**: Manejador centralizado de excepciones REST ubicado en `common/exception`.
- **DuplicadoException**: Excepción de dominio que produce HTTP 409, reutilizada del módulo `common`.
- **RecursoNoEncontradoException**: Excepción de dominio que produce HTTP 404, reutilizada del módulo `common`.
- **JpaAuditing**: Mecanismo de Spring Data JPA que gestiona automáticamente `fecha_creacion` y `fecha_modificacion`.
- **Flyway**: Herramienta de migración de base de datos. La migración del módulo Persona debe ser `V2__create_personas_table.sql`.

---

## Requirements

### Requirement 1: Registro de persona

**User Story:** Como operador del sistema, quiero registrar una nueva persona con sus datos de identificación documental y datos personales, para que quede almacenada en el sistema con un identificador único generado automáticamente.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud `POST /api/personas` con todos los campos obligatorios válidos, THE Sistema SHALL crear el registro de la persona, persistirlo en la base de datos y devolver HTTP 201 con el encabezado `Location` apuntando a `/api/personas/{id}` y el cuerpo con la `PersonaResponse` del recurso creado.

2. WHEN el Sistema recibe una solicitud de registro, THE Sistema SHALL generar automáticamente el campo `id` (BIGSERIAL) y los campos `fecha_creacion` y `fecha_modificacion` mediante JpaAuditing, sin que el cliente los provea.

3. WHEN el Sistema recibe una solicitud de registro cuya combinación (tipo_documento, numero_documento, complemento) ya existe en la base de datos, THE Sistema SHALL rechazar la solicitud y devolver HTTP 409 con una `ApiError` que identifique el conflicto de unicidad documental.

4. WHEN el Sistema recibe una solicitud de registro cuya combinación (tipo_documento, numero_documento, complemento = NULL) ya existe en la base de datos con otro registro que también tiene complemento NULL, THE Sistema SHALL rechazar la solicitud y devolver HTTP 409, garantizando que NULL participa en la unicidad.

5. WHEN el Sistema recibe una solicitud `POST /api/personas` con campos obligatorios ausentes o con valores que violan las restricciones de validación, THE Sistema SHALL devolver HTTP 400 con una `ApiError` que incluya la lista de errores de campo (`errores`).

6. WHEN el Sistema registra una persona exitosamente, THE Sistema SHALL registrar en el log de nivel INFO el evento de creación incluyendo el `id` generado, sin registrar ningún dato sensible de la persona.

---

### Requirement 2: Consulta de persona por ID

**User Story:** Como operador del sistema, quiero consultar los datos completos de una persona por su identificador interno, para obtener toda la información registrada de esa persona.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud `GET /api/personas/{id}` con un ID correspondiente a una persona existente, THE Sistema SHALL devolver HTTP 200 con la `PersonaResponse` completa de esa persona.

2. WHEN el Sistema recibe una solicitud `GET /api/personas/{id}` con un ID que no corresponde a ninguna persona registrada, THE Sistema SHALL delegar en `GlobalExceptionHandler` lanzando `RecursoNoEncontradoException` y devolver HTTP 404 con una `ApiError` descriptiva.

3. WHEN el Sistema procesa una consulta por ID, THE Sistema SHALL ejecutar la operación con `@Transactional(readOnly = true)` para optimizar la consulta de solo lectura.

---

### Requirement 3: Consulta de persona por identidad documental

**User Story:** Como operador del sistema, quiero consultar una persona por su tipo de documento, número de documento y complemento, para encontrar a una persona cuando se conoce su documento de identidad pero no su ID interno.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud `GET /api/personas/documento` con los parámetros `tipoDocumento`, `numeroDocumento` y opcionalmente `complemento`, y la combinación corresponde a una persona existente, THE Sistema SHALL devolver HTTP 200 con la `PersonaResponse` completa de esa persona.

2. WHEN el Sistema recibe una solicitud `GET /api/personas/documento` con una combinación (tipoDocumento, numeroDocumento, complemento) que no corresponde a ninguna persona registrada, THE Sistema SHALL lanzar `RecursoNoEncontradoException` y devolver HTTP 404 con una `ApiError` descriptiva.

3. WHEN el Sistema recibe una solicitud `GET /api/personas/documento` sin el parámetro `complemento`, THE Sistema SHALL interpretar el complemento como ausente (NULL) y buscar registros donde el complemento sea NULL.

4. WHEN el Sistema recibe una solicitud `GET /api/personas/documento` sin los parámetros obligatorios `tipoDocumento` o `numeroDocumento`, THE Sistema SHALL devolver HTTP 400 indicando los parámetros faltantes.

5. WHEN el Sistema procesa una consulta por documento, THE Sistema SHALL ejecutar la operación con `@Transactional(readOnly = true)`.

---

### Requirement 4: Listado paginado de personas con filtros opcionales

**User Story:** Como operador del sistema, quiero listar personas con paginación y filtros opcionales, para explorar el registro de personas de manera eficiente.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud `GET /api/personas` sin filtros, THE Sistema SHALL devolver HTTP 200 con una `PageResponse<PersonaResponse>` que contenga la página de personas según los parámetros de paginación (`page`, `size`, `sort`) con sus valores por defecto de Spring Data.

2. WHEN el Sistema recibe una solicitud `GET /api/personas` con el filtro `nombres`, THE Sistema SHALL devolver únicamente las personas cuyo campo `nombres` contenga el valor indicado (búsqueda LIKE, insensible a mayúsculas).

3. WHEN el Sistema recibe una solicitud `GET /api/personas` con el filtro `apellidoPaterno`, THE Sistema SHALL devolver únicamente las personas cuyo campo `apellido_paterno` contenga el valor indicado (búsqueda LIKE, insensible a mayúsculas).

4. WHEN el Sistema recibe una solicitud `GET /api/personas` con el filtro `tipoDocumento`, THE Sistema SHALL devolver únicamente las personas cuyo tipo de documento sea exactamente el valor indicado.

5. WHEN el Sistema recibe una solicitud `GET /api/personas` con múltiples filtros simultáneos, THE Sistema SHALL aplicar todos los filtros de forma combinada (condición AND) y devolver solo las personas que cumplan todos los criterios.

6. WHEN el Sistema recibe una solicitud `GET /api/personas` y no existen personas que cumplan los criterios indicados, THE Sistema SHALL devolver HTTP 200 con una `PageResponse<PersonaResponse>` con la lista `contenido` vacía y `totalElementos` igual a cero.

7. WHEN el Sistema procesa un listado de personas, THE Sistema SHALL ejecutar la consulta con `@Transactional(readOnly = true)` y utilizar `JpaSpecificationExecutor` para construir los filtros dinámicos.

---

### Requirement 5: Modelo de datos y unicidad documental

**User Story:** Como arquitecto del sistema, quiero que la tabla `personas` tenga un modelo de datos coherente con las reglas de negocio y la infraestructura existente, para garantizar la integridad de los datos desde la capa de persistencia.

#### Acceptance Criteria

1. THE Sistema SHALL crear la tabla `personas` mediante la migración Flyway `V2__create_personas_table.sql` con los campos: `id` (BIGSERIAL, PK), `tipo_documento` (VARCHAR NOT NULL), `numero_documento` (VARCHAR(50) NOT NULL), `complemento` (VARCHAR(10), nullable), `fecha_nacimiento` (DATE NOT NULL), `apellido_paterno` (VARCHAR(100), nullable), `apellido_materno` (VARCHAR(100), nullable), `apellido_esposo` (VARCHAR(100), nullable), `nombres` (VARCHAR(200) NOT NULL), `genero` (VARCHAR(20) NOT NULL), `estado_civil` (VARCHAR(20) NOT NULL), `fecha_creacion` (TIMESTAMPTZ NOT NULL), `fecha_modificacion` (TIMESTAMPTZ NOT NULL).

2. THE Sistema SHALL aplicar una constraint de unicidad en la tabla `personas` sobre la combinación (tipo_documento, numero_documento, complemento) que permita que NULL en `complemento` participe en la unicidad (usando una expresión con `COALESCE` o `NULLS NOT DISTINCT` según la versión de PostgreSQL disponible).

3. THE Sistema SHALL aplicar constraints `CHECK` en la tabla `personas` para los campos enumerados: `tipo_documento` debe estar en ('CI', 'PASAPORTE', 'CEX', 'NIT'), `genero` debe estar en ('MASCULINO', 'FEMENINO', 'OTRO'), `estado_civil` debe estar en ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE').

4. THE Sistema SHALL crear índices en la tabla `personas` sobre los campos `tipo_documento`, `numero_documento` y `(tipo_documento, numero_documento, complemento)` para optimizar las consultas más frecuentes.

5. WHEN JpaAuditing gestiona las fechas de auditoría, THE Sistema SHALL persistir `fecha_creacion` al momento del primer guardado y `fecha_modificacion` en cada actualización, usando `OffsetDateTime` como tipo Java mapeado a `TIMESTAMPTZ` en PostgreSQL.

---

### Requirement 6: Documentación OpenAPI y validaciones de entrada

**User Story:** Como desarrollador consumidor de la API, quiero que todos los endpoints del módulo Persona estén documentados en Swagger UI y que los datos de entrada sean validados, para integrarme con la API de forma segura y predecible.

#### Acceptance Criteria

1. THE Sistema SHALL documentar el controlador del módulo Persona con la anotación `@Tag(name = "Personas")` y cada endpoint con `@Operation`, `@ApiResponses` y `@Parameter` siguiendo los estándares del proyecto.

2. THE Sistema SHALL validar mediante Bean Validation en `PersonaRequest` que: `nombres` no sea blank y tenga máximo 200 caracteres, `tipoDocumento` no sea null, `numeroDocumento` no sea blank y tenga máximo 50 caracteres, `complemento` tenga máximo 10 caracteres si está presente, `fechaNacimiento` no sea null y sea una fecha en el pasado, `genero` no sea null, `estadoCivil` no sea null.

3. WHEN el Sistema recibe campos que violan las restricciones de Bean Validation, THE Sistema SHALL delegar en `GlobalExceptionHandler` (handler `MethodArgumentNotValidException`) y devolver HTTP 400 con `ApiError` que incluya la lista `errores` con el detalle de cada campo inválido.

4. THE Sistema SHALL documentar en `PersonaRequest` y `PersonaResponse` cada campo con `@Schema` indicando descripción, ejemplo y restricciones, siguiendo el patrón establecido en `UsuarioRequest` y `UsuarioResponse`.

5. WHEN se consulta Swagger UI en `/swagger-ui.html`, THE Sistema SHALL mostrar el grupo "Personas" con los cuatro endpoints (POST crear, GET por ID, GET por documento, GET listar) incluyendo los modelos de request y response.

---

### Requirement 7: Logging y trazabilidad

**User Story:** Como operador de infraestructura, quiero que todas las operaciones del módulo Persona generen logs coherentes con los estándares del proyecto, para diagnosticar problemas y auditar el comportamiento del sistema.

#### Acceptance Criteria

1. THE Sistema SHALL declarar el logger en cada clase del módulo Persona como `private static final Logger log = LoggerFactory.getLogger(ClaseActual.class)`, sin usar `System.out.println` ni ninguna otra salida estándar.

2. WHEN el Sistema ejecuta una operación de registro exitosa, THE Sistema SHALL emitir un log de nivel INFO en el Service con el `id` generado y, en el Controller, los logs de inicio y fin de la operación según el patrón del módulo `usuario`.

3. WHEN el Sistema detecta un intento de registro con identidad documental duplicada, THE Sistema SHALL emitir un log de nivel WARN en el Service con el tipo de documento y número de documento involucrados, sin registrar el complemento si pudiera contener información sensible.

4. WHEN el Sistema no encuentra una persona en una consulta por ID o por documento, THE Sistema SHALL emitir un log de nivel WARN en el Service con los parámetros de búsqueda usados.

5. WHILE el `CorrelationIdFilter` propaga el `correlationId` en el MDC, THE Sistema SHALL incluir automáticamente el `correlationId` en todos los logs del módulo Persona sin necesidad de incluirlo manualmente en cada mensaje.
