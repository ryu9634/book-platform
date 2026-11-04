package com.ryu.ebook.user.global.config;

import com.ryu.ebook.global.security.ApiSecurityErrorHandler;
import com.ryu.ebook.user.global.security.LoginMemberDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
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

    // API 로그인(/api/v1/auth/login)에서 화면 로그인과 같은 방식으로 아이디와 비밀번호를 확인할 때 씁니다.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    // API(/api/**): 세션 없이 Authorization: Bearer <Access Token>(JWT)으로 인증합니다.
    // 세션 쿠키로 인증하지 않으므로 CSRF 보호가 필요 없습니다.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JsonMapper jsonMapper,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        ApiSecurityErrorHandler errorHandler = new ApiSecurityErrorHandler(jsonMapper);
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ebooks", "/api/v1/ebooks/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
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
                        .requestMatchers("/members/new").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ebooks", "/ebooks/*").permitAll()
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
