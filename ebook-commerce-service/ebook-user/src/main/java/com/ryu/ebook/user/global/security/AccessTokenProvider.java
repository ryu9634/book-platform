package com.ryu.ebook.user.global.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

// Access Token(JWT) 발급. 검증은 Spring Security 리소스 서버가 JwtDecoder로 처리합니다.
@Component
@RequiredArgsConstructor
public class AccessTokenProvider {

    private static final String ISSUER = "ebook-user";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    // sub에는 회원 ID만 넣습니다. 토큰은 누구나 디코딩해 볼 수 있으므로 이메일 같은 개인정보는 넣지 않습니다.
    public String issue(Long memberId, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(JwtConfig.ROLES_CLAIM, List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }
}
