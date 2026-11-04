package com.ryu.ebook.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("회원은 일반 회원 권한과 활성 상태로 시작하고, 탈퇴하면 행은 남고 상태만 바뀐다")
    void create_withdraw() {
        Member member = Member.create("reader01", "encoded", "홍길동", "reader01@example.com");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        member.withdraw();
        assertThat(member.isWithdrawn()).isTrue();
        assertThat(Member.createAdmin("admin01", "encoded", "관리자", "a@example.com").isAdmin()).isTrue();
    }
}
