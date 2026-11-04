package com.ryu.library.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.library.auth.domain.RefreshToken;
import com.ryu.library.global.config.ClockConfig;
import com.ryu.library.global.config.JpaConfig;
import com.ryu.library.support.TestcontainersConfiguration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class RefreshTokenRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("해시로 토큰을 찾는다")
    void findByTokenHash_저장된토큰_조회() {
        // given
        refreshTokenRepository.saveAndFlush(RefreshToken.issue(1L, "a".repeat(64), NOW.plusDays(7)));

        // when, then
        assertThat(refreshTokenRepository.findByTokenHash("a".repeat(64))).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("b".repeat(64))).isEmpty();
    }

    @Test
    @DisplayName("회원의 살아 있는 토큰만 한 번에 폐기하고, 다른 회원 토큰은 건드리지 않는다")
    void revokeAllByMemberId_해당회원만폐기() {
        // given
        refreshTokenRepository.save(RefreshToken.issue(1L, "1".repeat(64), NOW.plusDays(7)));
        refreshTokenRepository.save(RefreshToken.issue(1L, "2".repeat(64), NOW.plusDays(7)));
        refreshTokenRepository.saveAndFlush(RefreshToken.issue(2L, "3".repeat(64), NOW.plusDays(7)));

        // when
        int revoked = refreshTokenRepository.revokeAllByMemberId(1L, NOW);

        // then
        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenHash("1".repeat(64)).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByTokenHash("3".repeat(64)).orElseThrow().isRevoked()).isFalse();
    }
}
