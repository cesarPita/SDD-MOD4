---
inclusion: always
---

# Estandares de APIs REST y Documentacion OpenAPI — Spring Boot SDD

Este archivo define las reglas obligatorias para el diseno de APIs REST y su documentacion
con OpenAPI/Swagger. Kiro debe aplicarlas automaticamente en cada endpoint nuevo o modificado.

---

## 1. Diseno de APIs REST

### Convencion de URLs

- Usar **sustantivos en plural** para recursos.
- No usar verbos en las URLs — el metodo HTTP expresa la accion.
- Usar `kebab-case` si el nombre del recurso tiene varias palabras.

```
POST   /api/usuarios             → crear usuario
GET    /api/usuarios             → listar usuarios
GET    /api/usuarios/{id}        → obtener por ID
GET    /api/usuarios/username/{username}  → obtener por campo unico
PUT    /api/usuarios/{id}        → actualizar usuario
DELETE /api/usuarios/{id}        → eliminar (logico) usuario
```

Mal:
```
POST /api/crearUsuario       ← verbo en URL
GET  /api/getUsuario/{id}    ← verbo en URL
POST /api/usuario/delete     ← metodo incorrecto + verbo
```

### Metodos HTTP

| Metodo | Uso |
|--------|-----|
| `GET`    | Consultar recursos. Sin efecto secundario. |
| `POST`   | Crear un nuevo recurso. |
| `PUT`    | Actualizar un recurso existente (reemplazo completo de campos editables). |
| `PATCH`  | Actualizacion parcial (solo si el caso de uso lo requiere). |
| `DELETE` | Eliminar o desactivar un recurso. |

### Codigos HTTP

| Situacion | Codigo |
|-----------|--------|
| Consulta exitosa (GET, PUT) | 200 OK |
| Creacion exitosa (POST) | 201 Created + header `Location` |
| Operacion sin contenido (DELETE logico) | 204 No Content |
| Request invalido / validacion fallida | 400 Bad Request |
| Recurso no encontrado | 404 Not Found |
| Conflicto (duplicado, estado invalido) | 409 Conflict |
| Error interno inesperado | 500 Internal Server Error |

### Paginacion

Usar `Pageable` de Spring Data para endpoints de listado.
Devolver `PageResponse<T>` de `common/dto` — no inventar otra estructura de paginacion.

Parametros estandar: `page`, `size`, `sort`.

---

## 2. DTOs y contratos de API

- Nunca exponer entidades JPA directamente como request o response.
- Usar DTOs separados para entrada (`Request`) y salida (`Response`) cuando los datos difieran.
- Validar requests con Bean Validation (`@Valid` en el Controller).
- Documentar cada campo del DTO con `@Schema`.

---

## 3. Manejo de errores

- Usar el `GlobalExceptionHandler` existente en `common/exception`.
- No crear `@RestControllerAdvice` adicionales.
- Todas las respuestas de error usan `ApiError` de `common/dto`.
- Excepciones de dominio disponibles:
  - `RecursoNoEncontradoException` → 404
  - `DuplicadoException` → 409

---

## 4. Swagger / OpenAPI

### Configuracion existente

El proyecto ya tiene configurado OpenAPI. Antes de agregar cualquier cosa:

1. Verificar `config/OpenApiConfig.java` — bean `OpenAPI` con metadata del proyecto.
2. Verificar `pom.xml` — dependencia `springdoc-openapi-starter-webmvc-ui:3.1.0`.
3. **No agregar** una segunda configuracion ni otra version de springdoc.
4. **No agregar** springdoc 2.x — es incompatible con Spring Boot 4.x.

URLs disponibles con la app corriendo:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Spec JSON:  `http://localhost:8080/v3/api-docs`

### Anotaciones requeridas por Controller

#### Clase
```java
@Tag(name = "Nombre del modulo", description = "Descripcion del grupo de endpoints")
```

#### Metodo
```java
@Operation(
    summary = "Titulo corto del endpoint",
    description = "Descripcion detallada del comportamiento, reglas y restricciones."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "...",
        content = @Content(schema = @Schema(implementation = MiResponse.class))),
    @ApiResponse(responseCode = "400", description = "...",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "...",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "...",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})
```

#### Parametros de path y query
```java
@Parameter(description = "ID numerico del recurso", example = "1", required = true)
@PathVariable Long id

@Parameter(description = "Filtro parcial por nombre", example = "juan")
@RequestParam(required = false) String nombre
```

#### Parametro Pageable — ocultar del swagger, es implicito
```java
@Parameter(hidden = true) Pageable pageable
```

#### Request body
```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Datos requeridos para la operacion",
    required = true,
    content = @Content(schema = @Schema(implementation = MiRequest.class))
)
@Valid @RequestBody MiRequest request
```

### Anotaciones en DTOs

Todos los campos de DTOs publicos deben tener `@Schema`:

```java
@Schema(description = "Descripcion del campo", example = "valor de ejemplo")
private String campo;
```

Campos de solo escritura (passwords, tokens):
```java
@Schema(description = "Contrasena", accessMode = Schema.AccessMode.WRITE_ONLY)
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

### Lo que debe documentarse por endpoint

| Elemento | Obligatorio |
|----------|-------------|
| `@Tag` en la clase | Si |
| `@Operation` con summary | Si |
| `@Operation` con description | Si (cuando el comportamiento no es obvio) |
| `@ApiResponse` para cada codigo HTTP posible | Si |
| `@Parameter` para cada path variable | Si |
| `@Parameter` para cada query param | Si |
| `@Schema` en DTOs de request y response | Si |
| `@Parameter(hidden=true)` para Pageable | Si |

### Lo que NO debe hacerse

- No duplicar entre Javadoc y `@Operation` — elegir uno.
- No documentar endpoints internos de infraestructura (actuator, health).
- No usar anotaciones Swagger de la version 2 (`@Api`, `@ApiOperation`, `@ApiParam`) — son de springfox, incompatible con este proyecto.
- No agregar `@Schema` redundantes que repitan lo que ya es obvio por el nombre del campo.

---

## 5. Ejemplo de Controller completo

```java
@Tag(name = "Recursos", description = "Gestion de recursos del sistema")
@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    private static final Logger log = LoggerFactory.getLogger(RecursoController.class);
    private final RecursoService recursoService;

    @Operation(summary = "Crear recurso", description = "Registra un nuevo recurso.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Recurso creado",
            content = @Content(schema = @Schema(implementation = RecursoResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Recurso duplicado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<RecursoResponse> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos del nuevo recurso", required = true,
                content = @Content(schema = @Schema(implementation = RecursoRequest.class)))
            @Valid @RequestBody RecursoRequest request) {

        log.info("Creando recurso nombre={}", request.getNombre());
        RecursoResponse response = recursoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        log.info("Recurso creado exitosamente id={}", response.getId());
        return ResponseEntity.created(location).body(response);
    }
}
```

---

## 6. Regla principal — obligatoria

Antes de implementar cualquier endpoint nuevo:

1. Verificar si ya existe un endpoint equivalente.
2. Seguir la convencion de URLs REST de este documento.
3. Usar los DTOs, excepciones y `PageResponse` existentes.
4. Verificar que `OpenApiConfig` ya esta configurado — no agregar otro.
5. Documentar el endpoint con las anotaciones OpenAPI requeridas.
6. Verificar que el endpoint aparece correctamente en Swagger UI.
7. No colocar logica de negocio en el Controller.

Estas reglas se aplican automaticamente a todos los endpoints nuevos o modificados en el proyecto.