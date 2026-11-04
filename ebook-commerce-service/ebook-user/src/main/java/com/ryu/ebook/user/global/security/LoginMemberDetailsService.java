package com.ryu.ebook.user.global.security;

import com.ryu.ebook.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginMemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // 탈퇴 회원은 enabled=false로 돌려주고, 로그인 거부는 Spring Security가 처리합니다.
    @Override
    public UserDetails loadUserByUsername(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .map(LoginMember::from)
                .orElseThrow(() -> new UsernameNotFoundException("member not found"));
    }
}
