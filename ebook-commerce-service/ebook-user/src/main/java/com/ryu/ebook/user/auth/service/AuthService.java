package com.ryu.ebook.user.auth.service;

import com.ryu.ebook.auth.domain.RefreshToken;
import com.ryu.ebook.auth.repository.RefreshTokenRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.user.auth.dto.LoginRequest;
import com.ryu.ebook.user.auth.dto.TokenResponse;
import com.ryu.ebook.user.global.security.AccessTokenProvider;
import com.ryu.ebook.user.global.security.JwtProperties;
import com.ryu.ebook.user.global.security.LoginMember;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final AuthenticationManager authenticationManager;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    // 아이디와 비밀번호 확인은 화면 로그인과 같은 AuthenticationManager(탈퇴 회원 거부 포함)로 합니다.
    @Transactional
    public TokenResponse login(LoginRequest request) {
        LoginMember loginMember;
        try {
            loginMember = (LoginMember) authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.loginId(), request.password()))
                    .getPrincipal();
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String role = loginMember.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return issueTokens(loginMember.getMemberId(), role);
    }

    // 쓸 때마다 새 Refresh Token으로 바꿉니다. 이미 폐기된 토큰이 다시 오면 탈취로 보고 그 회원의 토큰을 모두 폐기합니다.
    // noRollbackFor: 탈취 의심 시 전체 폐기는 예외를 던지더라도 커밋되어야 합니다.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllByMemberId(token.getMemberId(), now);
            log.warn("revoked refresh token reused. memberId={}, revokedCount={}", token.getMemberId(), revoked);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (token.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(token.getMemberId())
                .filter(m -> !m.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        token.revoke(now);
        return issueTokens(member.getId(), member.getRole().name());
    }

    // 로그아웃은 Refresh Token만 폐기합니다. 이미 발급된 Access Token은 만료(30분)될 때까지 유효합니다.
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> token.revoke(LocalDateTime.now(clock)));
    }

    private TokenResponse issueTokens(Long memberId, String role) {
        String accessToken = accessTokenProvider.issue(memberId, role);
        String rawRefreshToken = newRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(RefreshToken.issue(memberId, hash(rawRefreshToken), expiresAt));
        return TokenResponse.bearer(accessToken, accessTokenProvider.accessTokenTtlSeconds(), rawRefreshToken);
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // DB에는 원문 대신 해시만 저장합니다. DB가 유출돼도 토큰을 그대로 쓸 수 없게 하기 위해서입니다.
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
