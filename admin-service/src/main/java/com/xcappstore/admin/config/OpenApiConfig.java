package com.xcappstore.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    private final String serverUrl;

    public OpenApiConfig(@Value("${admin.openapi.server-url}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Bean
    public OpenAPI adminOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("信创软件商店后台 API")
                .version("1.0.0")
                .description("软件上传、版本管理、审核流、上下架和操作审计后台接口")
                .contact(new Contact().name("xcappstore admin service")))
            .servers(List.of(new Server().url(serverUrl).description("本地开发环境")))
            .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
