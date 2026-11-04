package com.ryu.ebook.global.exception;

import com.ryu.ebook.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// API(@RestController) 요청 전용. 화면 요청용 @ControllerAdvice는 화면을 만들 때 따로 추가합니다.
// 없는 경로(404), 지원하지 않는 메서드(405)는 컨트롤러를 정하기 전에 나는 예외라 여기까지 오지 않고 Spring 기본 처리를 따릅니다.
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

    // 깨진 JSON, 타입이 맞지 않는 값, 빠진 파라미터는 클라이언트 잘못이므로 500이 아니라 400으로 응답합니다.
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("bad request. type={}, message={}", e.getClass().getSimpleName(), e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
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
