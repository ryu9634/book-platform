package com.ryu.library.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.message(messageArgs));
        this.errorCode = errorCode;
    }
}
