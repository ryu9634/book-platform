package com.ryu.library.global.security;

import com.ryu.library.global.common.ApiResponse;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

// API 요청이 인증 전(401)이나 권한 부족(403)으로 막히면 로그인 페이지로 보내지 않고 ApiResponse JSON으로 응답합니다.
// Security 필터는 컨트롤러보다 앞에서 동작하므로 GlobalExceptionHandler가 처리하지 못해 따로 둡니다.
// user, admin 앱의 SecurityConfig가 직접 생성해서 씁니다. (core 테스트에서 Security 없이 뜨도록 @Component로 두지 않음)
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ApiSecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail(ErrorResponse.of(errorCode))));
    }
}
