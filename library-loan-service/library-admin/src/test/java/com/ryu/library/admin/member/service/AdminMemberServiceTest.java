package com.ryu.library.admin.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.domain.MemberRole;
import com.ryu.library.member.domain.MemberStatus;
import com.ryu.library.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminMemberServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AdminMemberService service = new AdminMemberService(memberRepository, mock(LoanRepository.class), CLOCK);

    @Test
    @DisplayName("관리자가 회원을 정지하고 해제한다. 정지 일수는 1~365일이다")
    void 정지_해제() {
        // given
        Member member = MemberFixture.activeMember();
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));

        // when, then
        service.suspend(5L, 7);
        assertThat(member.getSuspendedUntil()).isEqualTo(LocalDate.now(CLOCK).plusDays(7));
        service.releaseSuspension(5L);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThatThrownBy(() -> service.suspend(5L, 0)).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("자기 자신의 권한은 바꿀 수 없다")
    void changeRole_자기자신_M007() {
        assertThatThrownBy(() -> service.changeRole(1L, 1L, MemberRole.USER))
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
    }

    @Test
    @DisplayName("다른 회원은 관리자로 지정할 수 있다")
    void changeRole_다른회원_변경() {
        Member member = MemberFixture.activeMember();
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));

        service.changeRole(1L, 5L, MemberRole.ADMIN);

        assertThat(member.isAdmin()).isTrue();
    }
}
