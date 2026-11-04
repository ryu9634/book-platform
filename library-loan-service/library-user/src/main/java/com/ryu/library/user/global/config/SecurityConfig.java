package com.ryu.library.user.global.config;

import com.ryu.library.global.security.ApiSecurityErrorHandler;
import com.ryu.library.user.global.security.LoginMemberDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

    // 어떤 사용자 조회 서비스와 비밀번호 암호화로 로그인할지 직접 지정합니다.
    // 자동 구성에 맡기면 UserDetailsService 빈이 여러 개일 때 로그인 처리기가 만들어지지 않습니다.
    @Bean
    public AuthenticationProvider authenticationProvider(
            LoginMemberDetailsService loginMemberDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(loginMemberDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // API(/api/**): 세션을 쓰지 않습니다. JWT 인증은 다음 단계에서 추가하고, 그 전까지는 공개 API만 열어 둡니다.
    // 세션 쿠키로 인증하지 않으므로 CSRF 보호가 필요 없습니다.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        ApiSecurityErrorHandler errorHandler = new ApiSecurityErrorHandler(jsonMapper);
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .build();
    }

    // 화면: 세션 기반 폼 로그인. CSRF 보호를 켜 두고, Thymeleaf의 th:action 폼에는 토큰이 자동으로 들어갑니다.
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginId")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error"))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
