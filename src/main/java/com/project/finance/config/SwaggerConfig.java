package com.project.finance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Finance Dashboard API",
        version = "1.0.0",
        description = "Backend REST API for the Finance Data Processing and Access Control Dashboard. " +
                      "Supports user management with role-based access control (VIEWER, ANALYST, ADMIN), " +
                      "financial records CRUD with filtering, and aggregated dashboard analytics."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Bearer token. Obtain via POST /auth/login and include as: Authorization: Bearer <token>"
)
public class SwaggerConfig {
}
