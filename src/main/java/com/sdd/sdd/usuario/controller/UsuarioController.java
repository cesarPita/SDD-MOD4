package com.sdd.sdd.usuario.controller;

import com.sdd.sdd.common.dto.ApiError;
import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.service.UsuarioService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controller REST para la gestion de usuarios.
 * Expone los endpoints del modulo de usuarios y delega la logica al {@link UsuarioService}.
 */
@Tag(name = "Usuarios", description = "Operaciones de gestion de usuarios: registro, consulta, actualizacion y eliminacion logica.")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ── POST /api/usuarios ────────────────────────────────────────────────────

    @Operation(
        summary = "Registrar un nuevo usuario",
        description = "Crea un nuevo usuario en el sistema. El username y el email deben ser unicos. " +
                      "La contrasena se almacena cifrada con BCrypt y nunca se devuelve en la respuesta."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuario creado exitosamente",
            headers = @Header(name = "Location", description = "URI del recurso creado, ej: /api/usuarios/1"),
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = UsuarioResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada invalidos o campos requeridos ausentes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El username o el email ya estan registrados en el sistema",
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
    public ResponseEntity<UsuarioResponse> registrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos del nuevo usuario. La contrasena es de solo escritura.",
                required = true,
                content = @Content(schema = @Schema(implementation = UsuarioRequest.class))
            )
            @Valid @RequestBody UsuarioRequest request) {

        log.info("Registrando usuario username={}", request.getUsername());
        UsuarioResponse response = usuarioService.registrar(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        log.info("Usuario registrado exitosamente id={} username={}", response.getId(), response.getUsername());
        return ResponseEntity.created(location).body(response);
    }

    // ── GET /api/usuarios ─────────────────────────────────────────────────────

    @Operation(
        summary = "Listar usuarios con filtros y paginacion",
        description = "Devuelve una pagina de usuarios. Soporta filtros opcionales por username (LIKE), " +
                      "email (LIKE) y estado (ACTIVO/INACTIVO). Los parametros de paginacion siguen la " +
                      "convencion de Spring Data: page, size, sort."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Listado devuelto exitosamente (puede estar vacio)",
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
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @Parameter(description = "Filtro parcial por username (insensible a mayusculas)", example = "jper")
            @RequestParam(required = false) String username,

            @Parameter(description = "Filtro parcial por email (insensible a mayusculas)", example = "example.com")
            @RequestParam(required = false) String email,

            @Parameter(description = "Filtro exacto por estado del usuario", schema = @Schema(allowableValues = {"ACTIVO", "INACTIVO"}))
            @RequestParam(required = false) EstadoUsuario estado,

            @Parameter(hidden = true) Pageable pageable) {

        log.info("Listando usuarios username={} email={} estado={} page={} size={}",
                username, email, estado, pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<UsuarioResponse> result = usuarioService.listar(username, email, estado, pageable);
        log.info("Listado completado totalElementos={}", result.getTotalElementos());
        return ResponseEntity.ok(result);
    }

    // ── GET /api/usuarios/{id} ────────────────────────────────────────────────

    @Operation(
        summary = "Obtener usuario por ID",
        description = "Devuelve los datos completos de un usuario a partir de su identificador numerico."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = UsuarioResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe un usuario con el ID indicado",
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
    public ResponseEntity<UsuarioResponse> obtenerPorId(
            @Parameter(description = "ID numerico del usuario", example = "1", required = true)
            @PathVariable Long id) {

        log.info("Consultando usuario id={}", id);
        UsuarioResponse response = usuarioService.obtenerPorId(id);
        log.info("Usuario encontrado id={} username={}", response.getId(), response.getUsername());
        return ResponseEntity.ok(response);
    }

    // ── GET /api/usuarios/username/{username} ─────────────────────────────────

    @Operation(
        summary = "Obtener usuario por username",
        description = "Devuelve los datos completos de un usuario a partir de su username exacto."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = UsuarioResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe un usuario con el username indicado",
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
    @GetMapping("/username/{username}")
    public ResponseEntity<UsuarioResponse> obtenerPorUsername(
            @Parameter(description = "Username exacto del usuario", example = "jperez", required = true)
            @PathVariable String username) {

        log.info("Consultando usuario username={}", username);
        UsuarioResponse response = usuarioService.obtenerPorUsername(username);
        log.info("Usuario encontrado id={} username={}", response.getId(), response.getUsername());
        return ResponseEntity.ok(response);
    }

    // ── PUT /api/usuarios/{id} ────────────────────────────────────────────────

    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza los campos permitidos de un usuario existente. " +
                      "El username no puede modificarse. Si el email cambia, se verifica que no este en uso por otro usuario."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = UsuarioResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada invalidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe un usuario con el ID indicado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                               schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El nuevo email ya esta en uso por otro usuario",
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
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @Parameter(description = "ID numerico del usuario a actualizar", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request) {

        log.info("Actualizando usuario id={}", id);
        UsuarioResponse response = usuarioService.actualizar(id, request);
        log.info("Usuario actualizado exitosamente id={}", id);
        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/usuarios/{id} ─────────────────────────────────────────────

    @Operation(
        summary = "Eliminacion logica de usuario",
        description = "Cambia el estado del usuario a INACTIVO sin eliminar el registro fisico de la base de datos."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Usuario desactivado exitosamente. Sin cuerpo en la respuesta."
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe un usuario con el ID indicado",
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLogico(
            @Parameter(description = "ID numerico del usuario a desactivar", example = "1", required = true)
            @PathVariable Long id) {

        log.info("Eliminacion logica solicitada id={}", id);
        usuarioService.eliminarLogico(id);
        log.info("Usuario desactivado exitosamente id={}", id);
        return ResponseEntity.noContent().build();
    }
}