package com.ryu.library.user.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class AccessTokenProviderTest {

    private static final Instant ISSUED = Instant.parse("2026-11-04T03:00:00Z");
    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties =
            new JwtProperties("test-secret-key-for-unit-test-only-0123456789", Duration.ofMinutes(30), Duration.ofDays(7));

    private AccessTokenProvider providerAt(Instant now) {
        return new AccessTokenProvider(jwtConfig.jwtEncoder(properties), properties, Clock.fixed(now, ZoneId.of("UTC")));
    }

    private JwtDecoder decoderAt(Instant now) {
        return jwtConfig.jwtDecoder(properties, Clock.fixed(now, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("발급한 토큰을 같은 키로 검증하면 회원 ID와 권한이 그대로 나온다")
    void issue_검증_회원ID와권한() {
        // given
        String token = providerAt(ISSUED).issue(7L, "USER");

        // when
        Jwt jwt = decoderAt(ISSUED.plusSeconds(60)).decode(token);

        // then
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getExpiresAt()).isEqualTo(ISSUED.plus(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰은 거부한다")
    void decode_만료토큰_거부() {
        // given
        String token = providerAt(ISSUED).issue(7L, "USER");

        // when, then (시계 오차 허용 60초를 넘긴 시점)
        assertThatThrownBy(() -> decoderAt(ISSUED.plus(Duration.ofMinutes(32))).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("서명 키가 32바이트보다 짧으면 앱이 시작되지 않는다")
    void properties_짧은키_시작거부() {
        assertThatThrownBy(() -> new JwtProperties("short", Duration.ofMinutes(30), Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class);
    }
}
