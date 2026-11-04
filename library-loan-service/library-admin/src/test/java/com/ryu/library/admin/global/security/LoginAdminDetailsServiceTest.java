package com.ryu.library.admin.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
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
class LoginAdminDetailsServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    LoginAdminDetailsService loginAdminDetailsService;

    @Test
    @DisplayName("관리자 회원은 ADMIN 권한으로 로그인 정보를 만든다")
    void loadUserByUsername_관리자_ADMIN권한() {
        // given
        Member admin = Member.createAdmin("librarian", "encoded", "사서", "librarian@example.com");
        given(memberRepository.findByLoginId("librarian")).willReturn(Optional.of(admin));

        // when
        UserDetails details = loginAdminDetailsService.loadUserByUsername("librarian");

        // then
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("일반 회원 아이디는 없는 계정과 똑같이 거부한다")
    void loadUserByUsername_일반회원_없는계정처럼거부() {
        // given
        given(memberRepository.findByLoginId("reader01")).willReturn(Optional.of(MemberFixture.activeMember()));

        // when, then
        assertThatThrownBy(() -> loginAdminDetailsService.loadUserByUsername("reader01"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
