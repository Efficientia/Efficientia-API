package com.example.efficientia.documento.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DocumentoOpenApiConfig {

    @Bean
    OpenAPI efficientiaOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info()
                .title("Efficientia API REST Principal")
                .version("v1")
                .description("Contrato público de relatórios, documentos e assinaturas da Efficientia.")
                .contact(new Contact().name("Equipe Efficientia")));
    }
}
