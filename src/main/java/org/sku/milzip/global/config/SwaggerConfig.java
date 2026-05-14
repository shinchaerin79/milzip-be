package org.sku.milzip.global.config;

import java.util.List;

import org.sku.milzip.global.config.properties.SwaggerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  private final SwaggerProperties swaggerProperties;

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    Server server =
        new Server().url(swaggerProperties.getUrl()).description(swaggerProperties.getName());

    return new OpenAPI()
        .info(new Info().title("Milzip API").description("Milzip 서비스 API 명세서").version("v1"))
        .servers(List.of(server))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme));
  }
}
