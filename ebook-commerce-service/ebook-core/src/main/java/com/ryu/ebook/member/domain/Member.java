package com.ryu.ebook.member.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MEMBER_SEQ_GENERATOR")
    @SequenceGenerator(name = "MEMBER_SEQ_GENERATOR", sequenceName = "MEMBER_SEQ", allocationSize = 50)
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(nullable = false, length = 30)
    private String loginId;

    // BCrypt 해시만 저장합니다. 평문 비밀번호는 엔티티에 들어오지 않습니다.
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    private Member(String loginId, String encodedPassword, String name, String email, MemberRole role) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = MemberStatus.ACTIVE;
    }

    public static Member create(String loginId, String encodedPassword, String name, String email) {
        return new Member(loginId, encodedPassword, name, email, MemberRole.USER);
    }

    public static Member createAdmin(String loginId, String encodedPassword, String name, String email) {
        return new Member(loginId, encodedPassword, name, email, MemberRole.ADMIN);
    }

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    public boolean isWithdrawn() {
        return status == MemberStatus.WITHDRAWN;
    }

    // 주문, 서재, 리뷰가 이 회원을 참조하므로 행은 지우지 않고 상태만 바꿉니다.
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
