package com.sdd.sdd.persona.controller;

import com.sdd.sdd.common.dto.ApiError;
import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.persona.dto.PersonaRequest;
import com.sdd.sdd.persona.dto.PersonaResponse;
import com.sdd.sdd.persona.entity.TipoDocumento;
import com.sdd.sdd.persona.service.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controller REST para el registro y consulta de personas naturales.
 * Expone los endpoints del módulo Persona y delega la lógica al {@link PersonaService}.
 */
@Tag(name = "Personas", description = "Registro y consulta de personas naturales")
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private static final Logger log = LoggerFactory.getLogger(PersonaController.class);

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    // ── POST /api/personas ────────────────────────────────────────────────────

    @Operation(
        summary = "Registrar una nueva persona",
        description = "Crea una nueva persona natural en el sistema. La combinación de " +
                      "(tipoDocumento, numeroDocumento, complemento) debe ser única. " +
                      "El id, fechaCreacion y fechaModificacion son generados automáticamente."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Persona creada exitosamente",
            headers = @Header(name = "Location", description = "URI del recurso creado, ej: /api/personas/1"),
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos o campos requeridos ausentes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "La combinación documental (tipoDocumento, numeroDocumento, complemento) ya está registrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno inesperado del servidor",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    public ResponseEntity<PersonaResponse> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos de la nueva persona. Todos los campos obligatorios deben estar presentes.",
                required = true,
                content = @Content(schema = @Schema(implementation = PersonaRequest.class))
            )
            @Valid @RequestBody PersonaRequest request) {

        log.info("Registrando persona tipoDocumento={} numeroDocumento={}",
                request.getTipoDocumento(), request.getNumeroDocumento());
        PersonaResponse response = personaService.crear(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        log.info("Persona registrada exitosamente id={}", response.getId());
        return ResponseEntity.created(location).body(response);
    }

    // ── GET /api/personas ─────────────────────────────────────────────────────

    @Operation(
        summary = "Listar personas con filtros y paginación",
        description = "Devuelve una página de personas. Soporta filtros opcionales por nombres (LIKE, " +
                      "insensible a mayúsculas), apellidoPaterno (LIKE, insensible a mayúsculas) y " +
                      "tipoDocumento (exacto). Los parámetros de paginación siguen la convención de " +
                      "Spring Data: page, size, sort."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Listado devuelto exitosamente (puede estar vacío)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = PageResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno inesperado del servidor",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public ResponseEntity<PageResponse<PersonaResponse>> listar(
            @Parameter(description = "Filtro parcial por nombres (insensible a mayúsculas)", example = "Juan")
            @RequestParam(required = false) String nombres,

            @Parameter(description = "Filtro parcial por apellido paterno (insensible a mayúsculas)", example = "García")
            @RequestParam(required = false) String apellidoPaterno,

            @Parameter(description = "Filtro exacto por tipo de documento",
                       schema = @Schema(allowableValues = {"CI", "PASAPORTE", "CEX", "NIT"}))
            @RequestParam(required = false) TipoDocumento tipoDocumento,

            @Parameter(hidden = true) Pageable pageable) {

        log.info("Listando personas nombres={} apellidoPaterno={} tipoDocumento={} page={} size={}",
                nombres, apellidoPaterno, tipoDocumento, pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<PersonaResponse> result = personaService.listar(nombres, apellidoPaterno, tipoDocumento, pageable);
        log.info("Listado completado totalElementos={}", result.getTotalElementos());
        return ResponseEntity.ok(result);
    }

    // ── GET /api/personas/{id} ────────────────────────────────────────────────

    @Operation(
        summary = "Obtener persona por ID",
        description = "Devuelve los datos completos de una persona a partir de su identificador interno."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Persona encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe una persona con el ID indicado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno inesperado del servidor",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> obtenerPorId(
            @Parameter(description = "ID numérico de la persona", example = "1", required = true)
            @PathVariable Long id) {

        log.info("Consultando persona id={}", id);
        PersonaResponse response = personaService.obtenerPorId(id);
        log.info("Persona encontrada id={}", response.getId());
        return ResponseEntity.ok(response);
    }

    // ── GET /api/personas/documento ───────────────────────────────────────────

    @Operation(
        summary = "Obtener persona por identidad documental",
        description = "Busca una persona usando su tipo de documento, número de documento y, " +
                      "opcionalmente, el complemento. Si el complemento no se provee, se busca " +
                      "un registro cuyo complemento sea NULL."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Persona encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros obligatorios ausentes (tipoDocumento o numeroDocumento)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe una persona con la combinación documental indicada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno inesperado del servidor",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/documento")
    public ResponseEntity<PersonaResponse> obtenerPorDocumento(
            @Parameter(description = "Tipo de documento de identidad",
                       schema = @Schema(allowableValues = {"CI", "PASAPORTE", "CEX", "NIT"}),
                       required = true)
            @RequestParam TipoDocumento tipoDocumento,

            @Parameter(description = "Número de documento de identidad", example = "12345678", required = true)
            @RequestParam String numeroDocumento,

            @Parameter(description = "Complemento del documento (opcional). Si se omite, se busca complemento NULL.",
                       example = "1A")
            @RequestParam(required = false) String complemento) {

        log.info("Consultando persona por documento tipoDocumento={} numeroDocumento={}",
                tipoDocumento, numeroDocumento);
        PersonaResponse response = personaService.obtenerPorDocumento(tipoDocumento, numeroDocumento, complemento);
        log.info("Persona encontrada por documento id={}", response.getId());
        return ResponseEntity.ok(response);
    }
}
