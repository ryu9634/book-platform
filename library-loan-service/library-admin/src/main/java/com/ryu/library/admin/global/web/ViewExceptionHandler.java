package com.ryu.library.admin.global.web;

import com.ryu.library.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// 화면 요청에서 난 비즈니스 예외를 오류 화면으로 보여 줍니다. API 요청은 GlobalExceptionHandler가 먼저 처리합니다.
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class ViewExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, HttpServletResponse response, Model model) {
        log.warn("business exception on view. code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        response.setStatus(e.getErrorCode().getStatus().value());
        model.addAttribute("message", e.getMessage());
        return "error/business";
    }
}
