package com.ryu.library.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("회원을 만들면 일반 회원 권한과 활성 상태로 시작한다")
    void create_일반회원_USER권한_ACTIVE상태() {
        // given, when
        Member member = Member.create("reader01", "encoded", "홍길동", "reader01@example.com", null);

        // then
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getSuspendedUntil()).isNull();
        assertThat(member.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("관리자를 만들면 관리자 권한으로 시작한다")
    void createAdmin_관리자_ADMIN권한() {
        // given, when
        Member admin = Member.createAdmin("librarian", "encoded", "사서", "librarian@example.com");

        // then
        assertThat(admin.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("탈퇴하면 탈퇴 상태가 되고 행은 그대로 남는다")
    void withdraw_활성회원_WITHDRAWN상태() {
        // given
        Member member = Member.create("reader01", "encoded", "홍길동", "reader01@example.com", null);

        // when
        member.withdraw();

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.isWithdrawn()).isTrue();
    }
}
