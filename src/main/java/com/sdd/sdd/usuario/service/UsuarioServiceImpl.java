package com.sdd.sdd.usuario.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Implementación de {@link UsuarioService}.
 *
 * <p>Contiene toda la lógica de negocio del módulo de usuarios.
 * El campo {@code password} nunca aparece en ninguna sentencia de log (RF-08-02).
 */
@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository repository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------------------------------------------------------
    // RF-01 · Registro
    // -------------------------------------------------------------------------

    /**
     * Registra un nuevo usuario.
     *
     * <ol>
     *   <li>Verifica que el {@code username} no esté duplicado (RF-01-03).</li>
     *   <li>Verifica que el {@code email} no esté duplicado (RF-01-04).</li>
     *   <li>Cifra la contraseña con BCrypt (RF-01-06).</li>
     *   <li>Persiste con {@code estado = ACTIVO} (RF-01-07).</li>
     * </ol>
     */
    @Override
    public UsuarioResponse registrar(UsuarioRequest request) {
        System.out.println("ingresa aqui");
        if (repository.existsByUsername(request.getUsername())) {
            throw new DuplicadoException(
                    "El username '" + request.getUsername() + "' ya está registrado.");
        }
        if (repository.existsByEmail(request.getEmail())) {
            throw new DuplicadoException(
                    "El email '" + request.getEmail() + "' ya está registrado.");
        }
        System.out.println("ing cifrado");
        String passwordCifrado = passwordEncoder.encode(request.getPassword());
        System.out.println("sal cifrado");
        Usuario usuario = UsuarioMapper.toEntity(request, passwordCifrado);
        System.out.println("sal usuario");
        Usuario guardado = repository.save(usuario);
        System.out.println("sal guardado");
        return UsuarioMapper.toResponse(guardado);
    }

    // -------------------------------------------------------------------------
    // RF-02 · Consulta
    // -------------------------------------------------------------------------

    /**
     * Obtiene un usuario por su {@code id} (RF-02-01).
     *
     * @throws RecursoNoEncontradoException si el id no existe
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con id " + id + " no encontrado."));
        return UsuarioMapper.toResponse(usuario);
    }

    /**
     * Obtiene un usuario por su {@code username} (RF-02-02).
     *
     * @throws RecursoNoEncontradoException si el username no existe
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorUsername(String username) {
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con username '" + username + "' no encontrado."));
        return UsuarioMapper.toResponse(usuario);
    }

    /**
     * Lista usuarios con filtros opcionales y paginación (RF-02-03, RF-02-04, RF-02-05).
     *
     * <p>Los filtros {@code username} y {@code email} usan LIKE insensible a mayúsculas.
     * El filtro {@code estado} usa igualdad exacta.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String username, String email,
                                                EstadoUsuario estado, Pageable pageable) {
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

    // -------------------------------------------------------------------------
    // RF-03 · Edición
    // -------------------------------------------------------------------------

    /**
     * Actualiza los campos permitidos de un usuario (RF-03-01 a RF-03-07).
     *
     * <ul>
     *   <li>{@code username} no es modificable (RF-03-03).</li>
     *   <li>Si el email cambia, verifica que no pertenezca a otro usuario (RF-03-04).</li>
     *   <li>{@code fechaModificacion} se actualiza automáticamente por {@code @LastModifiedDate} (RF-03-05).</li>
     * </ul>
     *
     * @throws RecursoNoEncontradoException si el id no existe
     * @throws DuplicadoException           si el nuevo email ya pertenece a otro usuario
     */
    @Override
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con id " + id + " no encontrado."));

        if (!usuario.getEmail().equalsIgnoreCase(request.getEmail())
                && repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicadoException(
                    "El email '" + request.getEmail() + "' ya está registrado.");
        }

        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setEmail(request.getEmail());
        usuario.setEstado(request.getEstado());

        Usuario actualizado = repository.save(usuario);
        return UsuarioMapper.toResponse(actualizado);
    }

    // -------------------------------------------------------------------------
    // RF-04 · Eliminación lógica
    // -------------------------------------------------------------------------

    /**
     * Realiza una eliminación lógica: cambia el {@code estado} a {@code INACTIVO}
     * sin borrar el registro físico (RF-04-01, RF-04-02, RF-04-03).
     *
     * @throws RecursoNoEncontradoException si el id no existe
     */
    @Override
    public void eliminarLogico(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario con id " + id + " no encontrado."));

        usuario.setEstado(EstadoUsuario.INACTIVO);
        repository.save(usuario);
    }
}
