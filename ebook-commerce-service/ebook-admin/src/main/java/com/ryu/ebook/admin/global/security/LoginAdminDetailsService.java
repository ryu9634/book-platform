package com.ryu.ebook.admin.global.security;

import com.ryu.ebook.member.domain.Member;
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
public class LoginAdminDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // 일반 회원 아이디는 "없는 계정"과 똑같이 처리합니다. 응답으로 일반 회원 아이디가 존재하는지 알 수 없게 하기 위해서입니다.
    @Override
    public UserDetails loadUserByUsername(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .filter(Member::isAdmin)
                .map(LoginAdmin::from)
                .orElseThrow(() -> new UsernameNotFoundException("admin not found"));
    }
}
