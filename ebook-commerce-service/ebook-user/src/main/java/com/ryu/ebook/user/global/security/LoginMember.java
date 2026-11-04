package com.ryu.ebook.user.global.security;

import com.ryu.ebook.member.domain.Member;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 세션에 저장되는 로그인 회원 정보. 엔티티 대신 필요한 값만 담아 세션에 엔티티가 들어가지 않게 합니다.
@Getter
public class LoginMember implements UserDetails {

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final String password;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    private LoginMember(Member member) {
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.name = member.getName();
        this.password = member.getPassword();
        this.enabled = !member.isWithdrawn();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    public static LoginMember from(Member member) {
        return new LoginMember(member);
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
