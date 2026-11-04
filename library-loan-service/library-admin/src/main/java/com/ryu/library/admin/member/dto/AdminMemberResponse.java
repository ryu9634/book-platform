package com.ryu.library.admin.member.dto;

import com.ryu.library.member.domain.Member;
import com.ryu.library.member.domain.MemberRole;
import com.ryu.library.member.domain.MemberStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long id,
        String loginId,
        String name,
        String email,
        MemberRole role,
        MemberStatus status,
        LocalDate suspendedUntil,
        long activeLoanCount,
        LocalDateTime createdAt) {

    public static AdminMemberResponse of(Member member, long activeLoanCount) {
        return new AdminMemberResponse(member.getId(), member.getLoginId(), member.getName(), member.getEmail(),
                member.getRole(), member.getStatus(), member.getSuspendedUntil(), activeLoanCount, member.getCreatedAt());
    }

    public String statusLabel() {
        return switch (status) {
            case ACTIVE -> "정상";
            case SUSPENDED -> "정지 (" + suspendedUntil + "까지)";
            case WITHDRAWN -> "탈퇴";
        };
    }
}
