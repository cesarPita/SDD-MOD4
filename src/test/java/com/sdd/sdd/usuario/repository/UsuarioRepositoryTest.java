package com.sdd.sdd.usuario.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.TestPropertySource;

import com.sdd.sdd.config.JpaAuditingConfig;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.entity.Usuario;

/**
 * Tests de repositorio para {@link UsuarioRepository}.
 *
 * Estrategia de base de datos:
 *   - No hay H2 ni Testcontainers en el classpath (Spring Boot 4.x no los incluye por defecto).
 *   - Se usa {@code replace = NONE} para conectar al PostgreSQL real (localhost:5444/sdd).
 *   - Flyway se deshabilita para el contexto de test; Hibernate crea/borra el esquema con
 *     {@code ddl-auto=create-drop} directamente desde las entidades.
 *   - {@code JpaAuditingConfig} se excluye del contexto de test para evitar que
 *     {@code @EnableJpaAuditing} se registre dos veces.
 *   - Se proporciona un {@code AuditingHandler} simulado que no hace nada, lo que evita
 *     la incompatibilidad de Spring Data al intentar convertir la fecha de auditoría
 *     a {@code OffsetDateTime} (tipo no soportado nativamente por el mecanismo de auditoría).
 *     Las fechas se asignan manualmente en el helper de construcción de entidades.
 *   - Cada test es {@code @Transactional} (comportamiento por defecto de @DataJpaTest),
 *     por lo que el rollback automático garantiza aislamiento entre métodos.
 *
 * Requisito de infraestructura: PostgreSQL debe estar disponible en localhost:5444/sdd
 * con las credenciales configuradas en application.properties.
 * Si se desea independencia total del entorno, se recomienda agregar Testcontainers:
 *   {@code org.springframework.boot:spring-boot-testcontainers} (scope test)
 *   {@code org.testcontainers:postgresql} (scope test)
 * y reemplazar las propiedades del datasource por {@code @ServiceConnection}.
 */
@DataJpaTest(excludeAutoConfiguration = JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(UsuarioRepositoryTest.NoOpAuditingConfig.class)
class UsuarioRepositoryTest {

    /**
     * Proporciona un {@link AuditingHandler} simulado (no-op) para evitar que
     * {@code AuditingEntityListener} intente convertir la hora de auditoría a
     * {@code OffsetDateTime}, tipo no soportado por el conversor de Spring Data.
     * Las fechas se asignan directamente en el helper {@code nuevoUsuario()}.
     */
    @TestConfiguration
    static class NoOpAuditingConfig {

        @Bean("jpaAuditingHandler")
        AuditingHandler jpaAuditingHandler() {
            AuditingHandler handler = mock(AuditingHandler.class);
            when(handler.markCreated(any())).thenAnswer(inv -> inv.getArgument(0));
            when(handler.markModified(any())).thenAnswer(inv -> inv.getArgument(0));
            return handler;
        }
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UsuarioRepository repository;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Usuario nuevoUsuario(String username, String email) {
        OffsetDateTime ahora = OffsetDateTime.now();
        return Usuario.builder()
                .nombres("Nombre Test")
                .apellidos("Apellido Test")
                .username(username)
                .email(email)
                .password("$2a$10$hashedpassword")
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(ahora)
                .fechaModificacion(ahora)
                .build();
    }

    // -----------------------------------------------------------------------
    // existsByUsername — RF-01-03
    // -----------------------------------------------------------------------

    @Test
    void existsByUsername_retorna_true() {
        // Arrange
        em.persistAndFlush(nuevoUsuario("juanito", "juanito@example.com"));

        // Act
        boolean resultado = repository.existsByUsername("juanito");

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    void existsByUsername_retorna_false() {
        // No se persiste ningún usuario con ese username

        // Act
        boolean resultado = repository.existsByUsername("noexiste");

        // Assert
        assertThat(resultado).isFalse();
    }

    // -----------------------------------------------------------------------
    // existsByEmail — RF-01-04
    // -----------------------------------------------------------------------

    @Test
    void existsByEmail_retorna_true() {
        // Arrange
        em.persistAndFlush(nuevoUsuario("maria", "maria@example.com"));

        // Act
        boolean resultado = repository.existsByEmail("maria@example.com");

        // Assert
        assertThat(resultado).isTrue();
    }

    // -----------------------------------------------------------------------
    // existsByEmailAndIdNot — RF-03-04
    // -----------------------------------------------------------------------

    @Test
    void existsByEmailAndIdNot_retorna_true_cuando_email_pertenece_a_otro_usuario() {
        // Arrange: dos usuarios distintos; luego simulamos que "lucia" quiere
        // cambiar su email al de "pedro" (que ya está tomado)
        em.persistAndFlush(nuevoUsuario("pedro", "pedro@example.com"));
        em.persistAndFlush(nuevoUsuario("lucia", "lucia@example.com"));

        Long idLucia = repository.findByUsername("lucia").orElseThrow().getId();

        // Act: ¿existe "pedro@example.com" en un registro distinto al de lucia?
        boolean resultado = repository.existsByEmailAndIdNot("pedro@example.com", idLucia);

        // Assert
        assertThat(resultado).isTrue();
    }

    // -----------------------------------------------------------------------
    // findByUsername — RF-02-02
    // -----------------------------------------------------------------------

    @Test
    void findByUsername_existente_retorna_optional_con_valor() {
        // Arrange
        em.persistAndFlush(nuevoUsuario("carlos", "carlos@example.com"));

        // Act
        Optional<Usuario> resultado = repository.findByUsername("carlos");

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("carlos");
        assertThat(resultado.get().getEmail()).isEqualTo("carlos@example.com");
    }

    @Test
    void findByUsername_inexistente_retorna_optional_vacio() {
        // Act
        Optional<Usuario> resultado = repository.findByUsername("fantasma");

        // Assert
        assertThat(resultado).isEmpty();
    }
}
