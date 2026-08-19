package com.sdd.sdd.usuario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.common.exception.DuplicadoException;
import com.sdd.sdd.common.exception.RecursoNoEncontradoException;
import com.sdd.sdd.config.SecurityConfig;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link UsuarioController}.
 * Covers all 16 scenarios (T-01 through T-17 from requirements.md).
 *
 * Spring Boot 4.x: @WebMvcTest is in org.springframework.boot.webmvc.test.autoconfigure
 * Spring 7.x: @MockBean replaced by @MockitoBean in org.springframework.test.context.bean.override.mockito
 */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UsuarioService usuarioService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private UsuarioResponse responseBase;
    private UsuarioRequest requestValido;
    private UsuarioUpdateRequest updateRequestValido;

    @BeforeEach
    void setUp() {
        responseBase = UsuarioResponse.builder()
                .id(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .username("jperez")
                .email("juan@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(OffsetDateTime.now())
                .fechaModificacion(OffsetDateTime.now())
                .build();

        requestValido = UsuarioRequest.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .username("jperez")
                .email("juan@example.com")
                .password("Secreto123")
                .build();

        updateRequestValido = UsuarioUpdateRequest.builder()
                .nombres("Juan Modificado")
                .apellidos("Pérez")
                .email("nuevo@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    // ── POST /api/usuarios ────────────────────────────────────────────────────

    /**
     * T-01 — Registro exitoso: HTTP 201, Location header, sin campo password.
     * RF-01-09, RF-08-03
     */
    @Test
    void post_registro_exitoso() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(responseBase);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.email").value("juan@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-02 — Username duplicado: HTTP 409, ApiError.
     * RF-01-03
     */
    @Test
    void post_username_duplicado() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class)))
                .thenThrow(new DuplicadoException("El username 'jperez' ya está en uso."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El username 'jperez' ya está en uso."))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-03 — Email duplicado: HTTP 409, ApiError.
     * RF-01-04
     */
    @Test
    void post_email_duplicado() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class)))
                .thenThrow(new DuplicadoException("El email 'juan@example.com' ya está en uso."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-04 — Datos inválidos (email mal formado): HTTP 400, ApiError con errores.
     * RF-07-05
     */
    @Test
    void post_datos_invalidos() throws Exception {
        UsuarioRequest requestInvalido = UsuarioRequest.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .username("jperez")
                .email("no-es-un-email")   // email inválido
                .password("Secreto123")
                .build();

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.errores[0].campo").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-05 — Campos obligatorios vacíos: HTTP 400, ApiError con errores.
     * RF-01-02
     */
    @Test
    void post_campos_obligatorios_vacios() throws Exception {
        UsuarioRequest requestVacio = UsuarioRequest.builder()
                .nombres("")       // @NotBlank falla
                .apellidos("")     // @NotBlank falla
                .username("")      // @NotBlank falla
                .email("")         // @NotBlank falla
                .password("")      // @NotBlank falla
                .build();

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVacio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── GET /api/usuarios/{id} ────────────────────────────────────────────────

    /**
     * T-06 — Consulta por ID existente: HTTP 200, UsuarioResponse.
     * RF-02-01
     */
    @Test
    void get_por_id_existente() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(responseBase);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-07 — Consulta por ID inexistente: HTTP 404, ApiError.
     * RF-02-01
     */
    @Test
    void get_por_id_inexistente() throws Exception {
        when(usuarioService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Usuario con id 99 no encontrado."));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── GET /api/usuarios/username/{username} ─────────────────────────────────

    /**
     * T-08 — Consulta por username existente: HTTP 200, UsuarioResponse.
     * RF-02-02
     */
    @Test
    void get_por_username_existente() throws Exception {
        when(usuarioService.obtenerPorUsername("jperez")).thenReturn(responseBase);

        mockMvc.perform(get("/api/usuarios/username/jperez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-09 — Consulta por username inexistente: HTTP 404, ApiError.
     * RF-02-02
     */
    @Test
    void get_por_username_inexistente() throws Exception {
        when(usuarioService.obtenerPorUsername("fantasma"))
                .thenThrow(new RecursoNoEncontradoException("Usuario 'fantasma' no encontrado."));

        mockMvc.perform(get("/api/usuarios/username/fantasma"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── GET /api/usuarios (listado paginado y filtros) ────────────────────────

    /**
     * T-15 — Listado paginado con parámetros válidos: HTTP 200, PageResponse con metadatos.
     * RF-02-03, RF-02-05
     */
    @Test
    void get_listado_paginado() throws Exception {
        PageResponse<UsuarioResponse> page = new PageResponse<>(
                List.of(responseBase),
                0,   // pagina
                10,  // tamano
                1L,  // totalElementos
                1,   // totalPaginas
                true // ultimo
        );

        when(usuarioService.listar(isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/usuarios")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.contenido[0].id").value(1L))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.ultimo").value(true))
                .andExpect(jsonPath("$.contenido[0].password").doesNotExist());
    }

    /**
     * T-16 — Filtro por estado ACTIVO: HTTP 200, solo usuarios activos.
     * RF-02-04
     */
    @Test
    void get_listado_filtro_estado() throws Exception {
        PageResponse<UsuarioResponse> page = new PageResponse<>(
                List.of(responseBase),
                0, 10, 1L, 1, true
        );

        when(usuarioService.listar(isNull(), isNull(), eq(EstadoUsuario.ACTIVO), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/usuarios")
                        .param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.contenido[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$.contenido[0].password").doesNotExist());
    }

    // ── PUT /api/usuarios/{id} ────────────────────────────────────────────────

    /**
     * T-10 — Modificación exitosa: HTTP 200, UsuarioResponse actualizado.
     * RF-03-07
     */
    @Test
    void put_actualizar_exitoso() throws Exception {
        UsuarioResponse actualizado = UsuarioResponse.builder()
                .id(1L)
                .nombres("Juan Modificado")
                .apellidos("Pérez")
                .username("jperez")
                .email("nuevo@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(OffsetDateTime.now())
                .fechaModificacion(OffsetDateTime.now())
                .build();

        when(usuarioService.actualizar(eq(1L), any(UsuarioUpdateRequest.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestValido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombres").value("Juan Modificado"))
                .andExpect(jsonPath("$.email").value("nuevo@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-11 — Modificación de usuario inexistente: HTTP 404, ApiError.
     * RF-03-06
     */
    @Test
    void put_actualizar_inexistente() throws Exception {
        when(usuarioService.actualizar(eq(99L), any(UsuarioUpdateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Usuario con id 99 no encontrado."));

        mockMvc.perform(put("/api/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestValido)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /**
     * T-12 — Modificación con email duplicado: HTTP 409, ApiError.
     * RF-03-04
     */
    @Test
    void put_email_duplicado() throws Exception {
        when(usuarioService.actualizar(eq(1L), any(UsuarioUpdateRequest.class)))
                .thenThrow(new DuplicadoException("El email 'nuevo@example.com' ya está en uso por otro usuario."));

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestValido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ── DELETE /api/usuarios/{id} ─────────────────────────────────────────────

    /**
     * T-13 — Eliminación lógica exitosa: HTTP 204, cuerpo vacío.
     * RF-04-05
     */
    @Test
    void delete_eliminacion_logica() throws Exception {
        doNothing().when(usuarioService).eliminarLogico(1L);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * T-14 — Eliminación lógica de usuario inexistente: HTTP 404, ApiError.
     * RF-04-04
     */
    @Test
    void delete_inexistente() throws Exception {
        doThrow(new RecursoNoEncontradoException("Usuario con id 99 no encontrado."))
                .when(usuarioService).eliminarLogico(99L);

        mockMvc.perform(delete("/api/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
