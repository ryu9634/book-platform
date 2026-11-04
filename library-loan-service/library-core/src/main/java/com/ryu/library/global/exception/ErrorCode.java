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
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "C008", "외부 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "일시적인 오류가 발생했습니다."),

    // 회원 (M)
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "M001", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "회원을 찾을 수 없습니다."),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "M004", "연체로 대출이 정지된 상태입니다. (%s까지)"),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "M005", "탈퇴한 회원입니다."),

    // 인증 (A)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "로그인이 만료되었습니다. 다시 로그인해 주세요."),

    // 도서 (B)
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "도서를 찾을 수 없습니다."),
    DUPLICATE_ISBN(HttpStatus.CONFLICT, "B002", "이미 등록된 ISBN입니다."),
    ISBN_NOT_FOUND_IN_NL(HttpStatus.NOT_FOUND, "B003", "국립중앙도서관에서 해당 ISBN의 도서를 찾을 수 없습니다."),
    NL_API_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "B004", "국립중앙도서관 API 인증키가 설정되지 않았습니다."),
    NL_API_INVALID_KEY(HttpStatus.SERVICE_UNAVAILABLE, "B006", "국립중앙도서관 API 인증키가 유효하지 않습니다. 공공데이터포털에서 키 상태를 확인해 주세요."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "B005", "분류를 찾을 수 없습니다."),
    COPY_NOT_FOUND(HttpStatus.NOT_FOUND, "B007", "소장 자료를 찾을 수 없습니다."),
    COPY_NOT_AVAILABLE(HttpStatus.CONFLICT, "B008", "대출할 수 없는 자료입니다. 이미 대출 중이거나 예약된 자료입니다."),
    COPY_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "B009", "현재 상태에서는 바꿀 수 없습니다."),

    // 대출 (L)
    LOAN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "L001", "최대 대출 권수(%s권)를 넘을 수 없습니다."),
    HAS_OVERDUE_LOAN(HttpStatus.BAD_REQUEST, "L002", "연체 중인 도서가 있어 대출할 수 없습니다. 먼저 반납해 주세요."),
    ALREADY_LOANED_BOOK(HttpStatus.CONFLICT, "L003", "이미 대출 중인 도서입니다."),
    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND, "L004", "대출 기록을 찾을 수 없습니다."),
    NO_AVAILABLE_COPY(HttpStatus.CONFLICT, "L005", "지금 대출할 수 있는 권이 없습니다. 예약해 주세요."),
    LOAN_ALREADY_RETURNED(HttpStatus.CONFLICT, "L006", "이미 반납한 도서입니다."),
    EXTEND_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "L007", "연장은 %s회까지 할 수 있습니다."),
    EXTEND_NOT_ALLOWED_OVERDUE(HttpStatus.BAD_REQUEST, "L008", "연체 중인 도서는 연장할 수 없습니다. 반납해 주세요."),
    EXTEND_NOT_ALLOWED_RESERVED(HttpStatus.BAD_REQUEST, "L009", "예약한 회원이 있어 연장할 수 없습니다."),

    // 예약 (R)
    RESERVATION_NOT_ALLOWED_AVAILABLE(HttpStatus.BAD_REQUEST, "R001", "대출 가능한 권이 있어 예약할 수 없습니다. 바로 대출해 주세요."),
    RESERVATION_NOT_ALLOWED_LOANED(HttpStatus.BAD_REQUEST, "R002", "이미 대출 중인 도서는 예약할 수 없습니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "R003", "이미 예약한 도서입니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "예약을 찾을 수 없습니다."),
    RESERVATION_NOT_CANCELABLE(HttpStatus.CONFLICT, "R005", "이미 끝난 예약입니다."),
    NO_COPY_TO_RESERVE(HttpStatus.BAD_REQUEST, "R006", "소장 자료가 없어 예약할 수 없습니다.");

    // 도메인 에러 코드는 기능을 만들 때 도메인별 접두사로 추가합니다. (A 인증, M 회원, B 도서, L 대출, R 예약)

    private final HttpStatus status;
    private final String code;
    private final String message;

    public String message(Object... args) {
        return String.format(message, args);
    }
}
