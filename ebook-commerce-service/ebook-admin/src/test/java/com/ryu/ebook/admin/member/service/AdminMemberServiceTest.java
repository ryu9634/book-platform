package com.ryu.ebook.admin.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminMemberServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AdminMemberService service = new AdminMemberService(memberRepository);

    @Test
    @DisplayName("관리자는 다른 회원을 탈퇴 처리하고, 자기 자신은 탈퇴 처리할 수 없다")
    void withdraw() {
        Member member = MemberFixture.activeMember();
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));

        service.withdraw(1L, 5L);

        assertThat(member.isWithdrawn()).isTrue();
        assertThatThrownBy(() -> service.withdraw(1L, 1L)).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
