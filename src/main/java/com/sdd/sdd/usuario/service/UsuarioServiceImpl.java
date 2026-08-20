package com.sdd.sdd.usuario.service;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.common.exception.DuplicadoException;
import com.sdd.sdd.common.exception.RecursoNoEncontradoException;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.entity.Usuario;
import com.sdd.sdd.usuario.mapper.UsuarioMapper;
import com.sdd.sdd.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion de {@link UsuarioService}.
 * El campo {@code password} nunca aparece en ninguna sentencia de log (RF-08-02).
 */
@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository repository, BCryptPasswordEncoder passwordEncoder) {
        this.repository      = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── RF-01 · Registro ──────────────────────────────────────────────────────

    @Override
    public UsuarioResponse registrar(UsuarioRequest request) {
        log.debug("Verificando disponibilidad de username={}", request.getUsername());

        if (repository.existsByUsername(request.getUsername())) {
            log.warn("Intento de registro con username duplicado: username={}", request.getUsername());
            throw new DuplicadoException(
                    "El username '" + request.getUsername() + "' ya esta registrado.");
        }
        if (repository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email duplicado: email={}", request.getEmail());
            throw new DuplicadoException(
                    "El email '" + request.getEmail() + "' ya esta registrado.");
        }

        String passwordCifrado = passwordEncoder.encode(request.getPassword());
        Usuario usuario = UsuarioMapper.toEntity(request, passwordCifrado);
        Usuario guardado = repository.save(usuario);

        log.info("Usuario creado correctamente id={} username={}", guardado.getId(), guardado.getUsername());
        return UsuarioMapper.toResponse(guardado);
    }

    // ── RF-02 · Consulta ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        log.debug("Buscando usuario por id={}", id);
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado id={}", id);
                    return new RecursoNoEncontradoException("Usuario con id " + id + " no encontrado.");
                });
        return UsuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorUsername(String username) {
        log.debug("Buscando usuario por username={}", username);
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado username={}", username);
                    return new RecursoNoEncontradoException(
                            "Usuario con username '" + username + "' no encontrado.");
                });
        return UsuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String username, String email,
                                                EstadoUsuario estado, Pageable pageable) {
        log.debug("Listando usuarios con filtros username={} email={} estado={}", username, email, estado);

        Specification<Usuario> spec = Specification.where((Specification<Usuario>) null);
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("estado"), estado));
        }

        return PageResponse.of(repository.findAll(spec, pageable).map(UsuarioMapper::toResponse));
    }

    // ── RF-03 · Edicion ───────────────────────────────────────────────────────

    @Override
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        log.debug("Actualizando usuario id={}", id);

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Intento de actualizacion de usuario inexistente id={}", id);
                    return new RecursoNoEncontradoException("Usuario con id " + id + " no encontrado.");
                });

        if (!usuario.getEmail().equalsIgnoreCase(request.getEmail())
                && repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            log.warn("Intento de actualizacion con email duplicado id={} email={}", id, request.getEmail());
            throw new DuplicadoException(
                    "El email '" + request.getEmail() + "' ya esta registrado.");
        }

        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setEmail(request.getEmail());
        usuario.setEstado(request.getEstado());

        Usuario actualizado = repository.save(usuario);
        log.info("Usuario actualizado correctamente id={}", id);
        return UsuarioMapper.toResponse(actualizado);
    }

    // ── RF-04 · Eliminacion logica ────────────────────────────────────────────

    @Override
    public void eliminarLogico(Long id) {
        log.debug("Ejecutando eliminacion logica id={}", id);

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Intento de eliminacion de usuario inexistente id={}", id);
                    return new RecursoNoEncontradoException("Usuario con id " + id + " no encontrado.");
                });

        usuario.setEstado(EstadoUsuario.INACTIVO);
        repository.save(usuario);
        log.info("Usuario desactivado (eliminacion logica) id={}", id);
    }
}