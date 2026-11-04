package com.ryu.library.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.member.MemberFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberSuspensionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Test
    @DisplayName("정지 중이면 정지 종료일을 담아 대출을 거부한다")
    void validateCanBorrow_정지중_M004() {
        // given
        Member member = MemberFixture.activeMember();
        member.suspendUntil(TODAY.plusDays(3));

        // when, then
        assertThatThrownBy(() -> member.validateCanBorrow(TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2026-11-07")
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_SUSPENDED);
    }

    @Test
    @DisplayName("정지 종료일이 지나면 따로 해제하지 않아도 대출할 수 있다")
    void validateCanBorrow_정지만료_자동해제() {
        // given
        Member member = MemberFixture.activeMember();
        member.suspendUntil(TODAY.minusDays(1));

        // when
        member.validateCanBorrow(TODAY);

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("이미 더 늦게까지 정지돼 있으면 짧은 정지로 앞당기지 않는다")
    void suspendUntil_더늦은날짜유지() {
        // given
        Member member = MemberFixture.activeMember();
        member.suspendUntil(TODAY.plusDays(10));

        // when
        member.suspendUntil(TODAY.plusDays(2));

        // then
        assertThat(member.getSuspendedUntil()).isEqualTo(TODAY.plusDays(10));
    }

    @Test
    @DisplayName("탈퇴 회원은 대출할 수 없다")
    void validateCanBorrow_탈퇴_M005() {
        assertThatThrownBy(() -> MemberFixture.withdrawnMember().validateCanBorrow(TODAY))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
    }
}
