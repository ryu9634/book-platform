package com.ryu.library.user.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    private static final int MIN_SECRET_BYTES = 32;

    // 키가 비었거나 짧으면 앱 시작을 막습니다. 약한 키로 토큰이 발급되는 상황을 만들지 않기 위해서입니다.
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("library.jwt.secret은 32바이트 이상이어야 합니다. (환경 변수 LIBRARY_JWT_SECRET)");
        }
    }
}
