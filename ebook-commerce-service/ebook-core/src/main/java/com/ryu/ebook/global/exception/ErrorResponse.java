package com.ryu.ebook.global.exception;

import java.util.List;
import org.springframework.validation.BindingResult;

public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}
