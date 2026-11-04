package com.ryu.ebook.member;

import com.ryu.ebook.member.domain.Member;

public final class MemberFixture {

    // 엔티티는 해시 형식을 검사하지 않으므로 테스트에서는 의미 없는 문자열을 씁니다.
    public static final String ENCODED_PASSWORD = "encoded-password";

    private MemberFixture() {
    }

    public static Member activeMember() {
        return activeMember("reader01", "reader01@example.com");
    }

    public static Member activeMember(String loginId, String email) {
        return Member.create(loginId, ENCODED_PASSWORD, "홍길동", email);
    }

    public static Member withdrawnMember() {
        Member member = activeMember();
        member.withdraw();
        return member;
    }
}
