package com.ryu.library.user.loan.dto;

import java.time.LocalDate;

// 도서 상세 화면의 버튼 분기. 로그인한 회원 기준으로 계산합니다.
public record BookActionState(Type type, LocalDate suspendedUntil) {

    public enum Type {
        CAN_LOAN,
        ALREADY_LOANED,
        NO_AVAILABLE_COPY,
        SUSPENDED
    }

    public static BookActionState of(Type type) {
        return new BookActionState(type, null);
    }

    public static BookActionState suspended(LocalDate until) {
        return new BookActionState(Type.SUSPENDED, until);
    }
}
