package com.ryu.ebook.global.exception;

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
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "M005", "탈퇴한 회원입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "M006", "현재 비밀번호가 올바르지 않습니다."),

    // 인증 (A)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "로그인이 만료되었습니다. 다시 로그인해 주세요."),

    // 전자책 (E)
    EBOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "전자책을 찾을 수 없습니다."),
    DUPLICATE_ISBN(HttpStatus.CONFLICT, "E002", "이미 등록된 ISBN입니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "E003", "판매가는 0원 이상, 정가 이하여야 합니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "E004", "%s"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "분류를 찾을 수 없습니다."),
    CATEGORY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "E006", "분류는 2단계까지만 만들 수 있습니다."),
    EBOOK_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "E007", "판매 중이 아닌 전자책입니다."),

    // 장바구니 (CT)
    ALREADY_IN_CART(HttpStatus.CONFLICT, "CT001", "이미 장바구니에 있는 전자책입니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CT002", "장바구니 항목을 찾을 수 없습니다."),

    // 서재 (S)
    ALREADY_OWNED(HttpStatus.CONFLICT, "S001", "이미 내 서재에 있는 전자책입니다."),
    BOOKSHELF_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "내 서재에서 찾을 수 없습니다."),
    CONTENT_NOT_READY(HttpStatus.CONFLICT, "S003", "본문 파일을 준비 중입니다. 잠시 후 다시 시도해 주세요."),

    // 주문 (O)
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문을 찾을 수 없습니다."),
    ORDER_NOT_PENDING(HttpStatus.CONFLICT, "O002", "결제 대기 중인 주문이 아닙니다. 이미 결제했거나 취소된 주문입니다."),
    EMPTY_ORDER(HttpStatus.BAD_REQUEST, "O003", "주문할 전자책을 골라 주세요."),
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "O004", "%s"),

    // 결제 (P)
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P001", "결제 금액이 주문 금액과 다릅니다."),
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "P002", "결제에 실패했습니다. (%s)"),

    // 리뷰 (RV)
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "RV001", "구매한 회원만 리뷰를 쓸 수 있습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "RV002", "이미 리뷰를 쓴 전자책입니다. 기존 리뷰를 수정해 주세요."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "RV003", "리뷰를 찾을 수 없습니다.");

    // 도메인 에러 코드는 기능을 만들 때 도메인별 접두사로 추가합니다. (E 전자책, CT 장바구니, O 주문, P 결제, S 서재, RV 리뷰)

    private final HttpStatus status;
    private final String code;
    private final String message;

    public String message(Object... args) {
        return String.format(message, args);
    }
}
