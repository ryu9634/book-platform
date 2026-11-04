package com.ryu.library.global.exception;

import com.ryu.library.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// API(@RestController) 요청 전용. 화면 요청용 @ControllerAdvice는 화면을 만들 때 따로 추가합니다.
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("business exception. code={}, message={}", errorCode.getCode(), e.getMessage());
        return toResponse(errorCode, ErrorResponse.of(errorCode, e.getMessage()));
    }

    // MethodArgumentNotValidException도 BindException의 하위 타입이라 여기서 함께 처리됩니다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return toResponse(errorCode, ErrorResponse.of(errorCode, e.getBindingResult()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("optimistic lock conflict. message={}", e.getMessage());
        return toResponse(ErrorCode.CONCURRENT_MODIFICATION, ErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("data integrity violation. message={}", e.getMostSpecificCause().getMessage());
        return toResponse(ErrorCode.DUPLICATE_RESOURCE, ErrorResponse.of(ErrorCode.DUPLICATE_RESOURCE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("unexpected exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, ErrorResponse errorResponse) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorResponse));
    }
}
