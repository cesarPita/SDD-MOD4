package com.sdd.sdd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Configuracion de auditoria JPA.
 *
 * Registra un DateTimeProvider que devuelve OffsetDateTime, resolviendo el error
 * "Cannot convert unsupported date type java.time.LocalDateTime to java.time.OffsetDateTime"
 * que ocurre cuando Spring Data intenta poblar @CreatedDate/@LastModifiedDate
 * de la entidad Usuario (declarados como OffsetDateTime).
 *
 * Separada de SddApplication para que los tests @DataJpaTest puedan excluirla.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}