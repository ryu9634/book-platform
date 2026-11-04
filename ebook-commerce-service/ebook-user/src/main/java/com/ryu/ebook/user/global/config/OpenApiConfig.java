package com.ryu.ebook.user.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI에서 오른쪽 위 Authorize에 /api/v1/auth/login으로 받은 Access Token을 넣으면 인증이 필요한 API도 호출할 수 있습니다.
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "ebook 커머스 API", version = "v1", description = "회원 API. 응답은 ApiResponse 형식입니다."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
