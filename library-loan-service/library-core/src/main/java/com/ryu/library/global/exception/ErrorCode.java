package com.ryu.library.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 정보를 찾을 수 없습니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "C003", "다른 요청이 먼저 처리되었습니다. 다시 시도해 주세요."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C004", "이미 존재하는 정보입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C005", "지원하지 않는 요청 형식입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C006", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C007", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "일시적인 오류가 발생했습니다."),

    // 회원 (M)
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "M001", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "회원을 찾을 수 없습니다."),

    // 인증 (A)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "로그인이 만료되었습니다. 다시 로그인해 주세요.");

    // 도메인 에러 코드는 기능을 만들 때 도메인별 접두사로 추가합니다. (A 인증, M 회원, B 도서, L 대출, R 예약)

    private final HttpStatus status;
    private final String code;
    private final String message;

    public String message(Object... args) {
        return String.format(message, args);
    }
}
