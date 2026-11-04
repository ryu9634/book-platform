package com.ryu.ebook.admin.global.security;

import com.ryu.ebook.member.domain.Member;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 관리자 세션에 저장되는 로그인 정보. user 앱의 LoginMember와 세션을 공유하지 않습니다.
@Getter
public class LoginAdmin implements UserDetails {

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final String password;
    private final boolean enabled;

    private LoginAdmin(Member member) {
        this.memberId = member.getId();
        this.loginId = member.getLoginId();
        this.name = member.getName();
        this.password = member.getPassword();
        this.enabled = !member.isWithdrawn();
    }

    public static LoginAdmin from(Member member) {
        return new LoginAdmin(member);
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
