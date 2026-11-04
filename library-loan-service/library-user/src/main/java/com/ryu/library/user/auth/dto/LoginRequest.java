package com.ryu.library.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "아이디를 입력해 주세요.") String loginId,
        @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {

    // 비밀번호가 로그에 찍히지 않도록 toString에서 뺍니다.
    @Override
    public String toString() {
        return "LoginRequest[loginId=" + loginId + "]";
    }
}
