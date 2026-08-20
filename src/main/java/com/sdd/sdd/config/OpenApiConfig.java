package com.sdd.sdd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion central de OpenAPI / Swagger.
 *
 * <p>Expone la documentacion interactiva en:
 * <ul>
 *   <li>Swagger UI : {@code /swagger-ui.html}</li>
 *   <li>JSON spec  : {@code /v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SDD — Gestion de Usuarios API")
                        .description("API REST para la gestion de usuarios: registro, consulta, actualizacion y eliminacion logica.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SDD Team"))
                        .license(new License()
                                .name("Privado")));
    }
}