package com.sdd.sdd.usuario.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
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

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.service.UsuarioService;

import jakarta.validation.Valid;

/**
 * REST controller for user management endpoints.
 * Contains no business logic — all operations are delegated to UsuarioService.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * POST /api/usuarios — Register a new user.
     * Returns HTTP 201 with Location header pointing to the created resource.
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        System.out.println("Crear usuario");
        UsuarioResponse response = usuarioService.registrar(request);   
      
        System.out.println("Sale usuario"+response.toString());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * GET /api/usuarios — List users with optional filters and pagination.
     * Supports query params: username, email, estado, page, size, sort.
     */
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) EstadoUsuario estado,
            Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(username, email, estado, pageable));
    }

    /**
     * GET /api/usuarios/{id} — Retrieve a user by ID.
     * Returns HTTP 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    /**
     * GET /api/usuarios/username/{username} — Retrieve a user by username.
     * Returns HTTP 404 if not found.
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UsuarioResponse> obtenerPorUsername(@PathVariable String username) {
        return ResponseEntity.ok(usuarioService.obtenerPorUsername(username));
    }

    /**
     * PUT /api/usuarios/{id} — Update an existing user.
     * Returns HTTP 404 if not found, HTTP 409 if email already in use by another user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    /**
     * DELETE /api/usuarios/{id} — Logical deletion (sets estado to INACTIVO).
     * Returns HTTP 204 with no body. Returns HTTP 404 if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLogico(@PathVariable Long id) {
        usuarioService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }
}
