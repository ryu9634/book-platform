package com.ryu.ebook.auth.repository;

import com.ryu.ebook.auth.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 폐기된 토큰이 다시 쓰이면 탈취로 보고 그 회원의 살아 있는 토큰을 한 번에 모두 폐기합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.memberId = :memberId and t.revokedAt is null")
    int revokeAllByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
