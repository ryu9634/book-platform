package com.ryu.ebook.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.auth.domain.RefreshToken;
import com.ryu.ebook.auth.repository.RefreshTokenRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.user.auth.dto.LoginRequest;
import com.ryu.ebook.user.auth.dto.TokenResponse;
import com.ryu.ebook.user.global.security.AccessTokenProvider;
import com.ryu.ebook.user.global.security.JwtProperties;
import com.ryu.ebook.user.global.security.LoginMember;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AccessTokenProvider accessTokenProvider = mock(AccessTokenProvider.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final JwtProperties jwtProperties =
            new JwtProperties("test-secret-key-for-unit-test-only-0123456789", Duration.ofMinutes(30), Duration.ofDays(7));

    private AuthService authService;
    private Member member;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, accessTokenProvider, refreshTokenRepository,
                memberRepository, jwtProperties, CLOCK);
        member = MemberFixture.activeMember();
        ReflectionTestUtils.setField(member, "id", 1L);
        given(accessTokenProvider.issue(anyLong(), anyString())).willReturn("access-token");
        given(accessTokenProvider.accessTokenTtlSeconds()).willReturn(1800L);
    }

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token을 주고, DB에는 Refresh Token 해시만 저장한다")
    void login_성공_토큰발급_해시저장() {
        // given
        LoginMember principal = LoginMember.from(member);
        given(authenticationManager.authenticate(any()))
                .willReturn(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        // when
        TokenResponse response = authService.login(new LoginRequest("reader01", "plain-password"));

        // then
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(saved.getTokenHash()).isEqualTo(AuthService.hash(response.refreshToken())).isNotEqualTo(response.refreshToken());
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusDays(7));
        verify(accessTokenProvider).issue(1L, "USER");
    }

    @Test
    @DisplayName("아이디나 비밀번호가 틀리면 A001로 거부한다")
    void login_틀린비밀번호_A001() {
        // given
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("bad"));

        // when, then
        assertThatThrownBy(() -> authService.login(new LoginRequest("reader01", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refresh Token으로 갱신하면 쓴 토큰은 폐기하고 새 토큰을 발급한다")
    void refresh_정상토큰_교체() {
        // given
        RefreshToken old = RefreshToken.issue(1L, AuthService.hash("old-token"), NOW.plusDays(1));
        given(refreshTokenRepository.findByTokenHash(AuthService.hash("old-token"))).willReturn(Optional.of(old));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        TokenResponse response = authService.refresh("old-token");

        // then
        assertThat(old.isRevoked()).isTrue();
        assertThat(response.refreshToken()).isNotEqualTo("old-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이미 폐기된 Refresh Token이 다시 오면 탈취로 보고 그 회원의 토큰을 모두 폐기한다")
    void refresh_폐기된토큰재사용_전체폐기() {
        // given
        RefreshToken reused = RefreshToken.issue(1L, AuthService.hash("stolen"), NOW.plusDays(1));
        reused.revoke(NOW.minusMinutes(5));
        given(refreshTokenRepository.findByTokenHash(AuthService.hash("stolen"))).willReturn(Optional.of(reused));

        // when, then
        assertThatThrownBy(() -> authService.refresh("stolen"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository).revokeAllByMemberId(1L, NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료되었거나 없는 Refresh Token이면 A002로 거부한다")
    void refresh_만료또는없음_A002() {
        // given
        RefreshToken expired = RefreshToken.issue(1L, AuthService.hash("expired"), NOW);
        given(refreshTokenRepository.findByTokenHash(AuthService.hash("expired"))).willReturn(Optional.of(expired));

        // when, then
        assertThatThrownBy(() -> authService.refresh("expired"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        assertThatThrownBy(() -> authService.refresh("unknown"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token을 폐기한다")
    void logout_토큰폐기() {
        // given
        RefreshToken token = RefreshToken.issue(1L, AuthService.hash("token"), NOW.plusDays(1));
        given(refreshTokenRepository.findByTokenHash(AuthService.hash("token"))).willReturn(Optional.of(token));

        // when
        authService.logout("token");

        // then
        assertThat(token.isRevoked()).isTrue();
    }
}
