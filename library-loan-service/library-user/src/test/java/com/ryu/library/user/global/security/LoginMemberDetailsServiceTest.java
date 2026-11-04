package com.ryu.library.user.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class LoginMemberDetailsServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    LoginMemberDetailsService loginMemberDetailsService;

    @Test
    @DisplayName("가입한 회원은 일반 회원 권한으로 로그인 정보를 만든다")
    void loadUserByUsername_가입회원_USER권한() {
        // given
        given(memberRepository.findByLoginId("reader01")).willReturn(Optional.of(MemberFixture.activeMember()));

        // when
        UserDetails details = loginMemberDetailsService.loadUserByUsername("reader01");

        // then
        assertThat(details.getUsername()).isEqualTo("reader01");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("탈퇴한 회원은 로그인할 수 없는 상태로 돌려준다")
    void loadUserByUsername_탈퇴회원_비활성() {
        // given
        given(memberRepository.findByLoginId("reader01")).willReturn(Optional.of(MemberFixture.withdrawnMember()));

        // when
        UserDetails details = loginMemberDetailsService.loadUserByUsername("reader01");

        // then
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("없는 아이디면 예외를 던진다")
    void loadUserByUsername_없는아이디_예외() {
        // given
        given(memberRepository.findByLoginId("nobody")).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> loginMemberDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
