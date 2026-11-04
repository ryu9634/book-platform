package com.ryu.library.user.member.dto;

import com.ryu.library.member.domain.Member;

public record MemberResponse(Long id, String loginId, String name, String email) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLoginId(), member.getName(), member.getEmail());
    }
}
