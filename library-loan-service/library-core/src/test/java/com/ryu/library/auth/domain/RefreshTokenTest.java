package com.ryu.library.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);

    @Test
    @DisplayName("만료 시각이 되면 만료된 토큰이다")
    void isExpired_만료시각기준() {
        // given
        RefreshToken token = RefreshToken.issue(1L, "hash", NOW.plusDays(7));

        // when, then
        assertThat(token.isExpired(NOW)).isFalse();
        assertThat(token.isExpired(NOW.plusDays(7))).isTrue();
    }

    @Test
    @DisplayName("폐기하면 폐기 시각이 기록되고, 다시 폐기해도 처음 시각이 유지된다")
    void revoke_두번폐기_처음시각유지() {
        // given
        RefreshToken token = RefreshToken.issue(1L, "hash", NOW.plusDays(7));

        // when
        token.revoke(NOW);
        token.revoke(NOW.plusHours(1));

        // then
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW);
    }
}
