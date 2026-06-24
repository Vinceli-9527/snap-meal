package com.snapmeal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI snapMealOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Snap Meal API").version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("adminToken", header("token"))
                        .addSecuritySchemes("userAuthentication", header("authentication")))
                .addSecurityItem(new SecurityRequirement().addList("adminToken").addList("userAuthentication"));
    }

    private SecurityScheme header(String name) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(name);
    }
}
