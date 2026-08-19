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
 * <p>Registra un {@link DateTimeProvider} que devuelve {@link OffsetDateTime},
 * resolviendo el error "Cannot convert unsupported date type java.time.LocalDateTime
 * to java.time.OffsetDateTime" que ocurre cuando Spring Data intenta poblar los
 * campos {@code @CreatedDate} / {@code @LastModifiedDate} de la entidad
 * {@code Usuario}, la cual declara esas fechas como {@code OffsetDateTime}.
 *
 * <p>Separada de {@code SddApplication} para permitir que las pruebas de slice
 * con {@code @DataJpaTest} puedan excluirla y proporcionar su propia
 * configuracion de auditoria compatible con el contexto de test.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
