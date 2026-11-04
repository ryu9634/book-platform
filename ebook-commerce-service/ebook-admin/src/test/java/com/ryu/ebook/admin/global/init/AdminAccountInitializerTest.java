package com.ryu.ebook.admin.global.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountInitializerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AdminAccountInitializer initializer(String loginId, String password) {
        return new AdminAccountInitializer(new AdminAccountProperties(loginId, password), memberRepository, passwordEncoder);
    }

    @Test
    @DisplayName("설정이 있고 같은 아이디가 없으면 비밀번호를 암호화해서 관리자 계정을 만든다")
    void run_설정있음_관리자생성() {
        // given
        given(memberRepository.existsByLoginId("librarian")).willReturn(false);
        given(passwordEncoder.encode("plain-password")).willReturn("encoded-password");

        // when
        initializer("librarian", "plain-password").run(null);

        // then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().isAdmin()).isTrue();
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("같은 아이디가 이미 있으면 다시 만들지 않는다")
    void run_이미있음_건너뜀() {
        // given
        given(memberRepository.existsByLoginId("librarian")).willReturn(true);

        // when
        initializer("librarian", "plain-password").run(null);

        // then
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정이 비어 있으면 계정을 만들지 않는다")
    void run_설정없음_건너뜀() {
        // when
        initializer("", "").run(null);

        // then
        verify(memberRepository, never()).existsByLoginId(any());
        verify(memberRepository, never()).save(any());
    }
}
