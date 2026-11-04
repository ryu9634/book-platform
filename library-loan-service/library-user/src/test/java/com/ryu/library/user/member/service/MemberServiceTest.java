package com.ryu.library.user.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.domain.MemberRole;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.user.member.dto.MemberResponse;
import com.ryu.library.user.member.dto.MemberSignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberService memberService;

    private final MemberSignupRequest request =
            new MemberSignupRequest("reader01", "plain-password", "홍길동", "reader01@example.com", null);

    @Test
    @DisplayName("가입하면 비밀번호를 암호화해서 일반 회원으로 저장한다")
    void signup_새회원_암호화후저장() {
        // given
        given(memberRepository.existsByLoginId("reader01")).willReturn(false);
        given(memberRepository.existsByEmail("reader01@example.com")).willReturn(false);
        given(passwordEncoder.encode("plain-password")).willReturn("encoded-password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        MemberResponse response = memberService.signup(request);

        // then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(MemberRole.USER);
        assertThat(response.loginId()).isEqualTo("reader01");
    }

    @Test
    @DisplayName("이미 사용 중인 아이디면 가입할 수 없다")
    void signup_중복아이디_예외() {
        // given
        given(memberRepository.existsByLoginId("reader01")).willReturn(true);

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 가입할 수 없다")
    void signup_중복이메일_예외() {
        // given
        given(memberRepository.existsByLoginId("reader01")).willReturn(false);
        given(memberRepository.existsByEmail("reader01@example.com")).willReturn(true);

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(memberRepository, never()).save(any());
    }
}
