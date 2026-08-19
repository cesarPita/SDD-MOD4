package com.sdd.sdd.usuario.service;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.common.exception.DuplicadoException;
import com.sdd.sdd.common.exception.RecursoNoEncontradoException;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.entity.Usuario;
import com.sdd.sdd.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl service;

    private Usuario usuarioActivo;
    private UsuarioRequest request;
    private UsuarioUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        OffsetDateTime ahora = OffsetDateTime.now();

        usuarioActivo = Usuario.builder()
                .id(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .username("jperez")
                .email("juan@example.com")
                .password("$2a$10$hashedpassword")
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(ahora)
                .fechaModificacion(ahora)
                .build();

        request = UsuarioRequest.builder()
                .nombres("Juan")
                .apellidos("Pérez")
                .username("jperez")
                .email("juan@example.com")
                .password("secreto123")
                .build();

        updateRequest = UsuarioUpdateRequest.builder()
                .nombres("Juan Modificado")
                .apellidos("Pérez Modificado")
                .email("juan.nuevo@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-01 · Registro — T-01, T-02, T-03
    // -------------------------------------------------------------------------

    /**
     * T-01 · RF-01-06 · RF-08-03
     * Registro exitoso: BCrypt es invocado y la respuesta no contiene password.
     */
    @Test
    void registrar_exitoso() {
        when(repository.existsByUsername("jperez")).thenReturn(false);
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedpassword");
        when(repository.save(any(Usuario.class))).thenReturn(usuarioActivo);

        UsuarioResponse result = service.registrar(request);

        // encode() debe haberse llamado exactamente una vez (RF-01-06)
        verify(passwordEncoder, times(1)).encode(any());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("jperez");
        assertThat(result.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);

        // Verificación directa: UsuarioResponse no declara campo password (RF-08-03)
        assertThat(UsuarioResponse.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    /**
     * T-02 · RF-01-03
     * Registro con username duplicado lanza DuplicadoException.
     */
    @Test
    void registrar_username_duplicado() {
        when(repository.existsByUsername("jperez")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("jperez");

        // No debe intentarse persistir ni cifrar nada
        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    /**
     * T-03 · RF-01-04
     * Registro con email duplicado lanza DuplicadoException.
     */
    @Test
    void registrar_email_duplicado() {
        when(repository.existsByUsername("jperez")).thenReturn(false);
        when(repository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("juan@example.com");

        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // -------------------------------------------------------------------------
    // RF-02 · Consulta — T-06, T-07, T-08, T-09
    // -------------------------------------------------------------------------

    /**
     * T-06 · RF-02-01
     * Consulta por ID existente devuelve UsuarioResponse.
     */
    @Test
    void obtenerPorId_existente() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioActivo));

        UsuarioResponse result = service.obtenerPorId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("jperez");
    }

    /**
     * T-07 · RF-02-01
     * Consulta por ID inexistente lanza RecursoNoEncontradoException.
     */
    @Test
    void obtenerPorId_inexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    /**
     * T-08 · RF-02-02
     * Consulta por username existente devuelve UsuarioResponse.
     */
    @Test
    void obtenerPorUsername_existente() {
        when(repository.findByUsername("jperez")).thenReturn(Optional.of(usuarioActivo));

        UsuarioResponse result = service.obtenerPorUsername("jperez");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("jperez");
    }

    /**
     * T-09 · RF-02-02
     * Consulta por username inexistente lanza RecursoNoEncontradoException.
     */
    @Test
    void obtenerPorUsername_inexistente() {
        when(repository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorUsername("noexiste"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("noexiste");
    }

    // -------------------------------------------------------------------------
    // RF-03 · Edición — T-10, T-11, T-12
    // -------------------------------------------------------------------------

    /**
     * T-10 · RF-03-01
     * Actualización exitosa: campos permitidos se modifican y se devuelve la respuesta actualizada.
     */
    @Test
    void actualizar_exitoso() {
        Usuario usuarioActualizado = Usuario.builder()
                .id(1L)
                .nombres("Juan Modificado")
                .apellidos("Pérez Modificado")
                .username("jperez")
                .email("juan.nuevo@example.com")
                .password("$2a$10$hashedpassword")
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(usuarioActivo.getFechaCreacion())
                .fechaModificacion(OffsetDateTime.now())
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        // email cambia → verificar que no pertenece a otro usuario
        when(repository.existsByEmailAndIdNot("juan.nuevo@example.com", 1L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenReturn(usuarioActualizado);

        UsuarioResponse result = service.actualizar(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getNombres()).isEqualTo("Juan Modificado");
        assertThat(result.getApellidos()).isEqualTo("Pérez Modificado");
        assertThat(result.getEmail()).isEqualTo("juan.nuevo@example.com");
        // username no debe haber cambiado (RF-03-03)
        assertThat(result.getUsername()).isEqualTo("jperez");

        // Verificar que save() fue llamado con los campos actualizados
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository, times(1)).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombres()).isEqualTo("Juan Modificado");
        assertThat(guardado.getEmail()).isEqualTo("juan.nuevo@example.com");
    }

    /**
     * T-11 · RF-03-06
     * Actualización de usuario inexistente lanza RecursoNoEncontradoException.
     */
    @Test
    void actualizar_inexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, updateRequest))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any());
    }

    /**
     * T-12 · RF-03-04
     * Actualización con email ya registrado en otro usuario lanza DuplicadoException.
     */
    @Test
    void actualizar_email_duplicado() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(repository.existsByEmailAndIdNot("juan.nuevo@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, updateRequest))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("juan.nuevo@example.com");

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // RF-04 · Eliminación lógica — T-13, T-14
    // -------------------------------------------------------------------------

    /**
     * T-13 · RF-04-01 · RF-04-02
     * Eliminación lógica: estado pasa a INACTIVO sin borrar el registro físico.
     */
    @Test
    void eliminarLogico_existente() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(repository.save(any(Usuario.class))).thenReturn(usuarioActivo);

        service.eliminarLogico(1L);

        // Verificar que save() fue llamado con estado INACTIVO (RF-04-02)
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.INACTIVO);

        // deleteById nunca debe haberse llamado (RF-04-01)
        verify(repository, never()).deleteById(any());
        verify(repository, never()).delete(any(Usuario.class));
    }

    /**
     * T-14 · RF-04-04
     * Eliminación lógica de usuario inexistente lanza RecursoNoEncontradoException.
     */
    @Test
    void eliminarLogico_inexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarLogico(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }
}
