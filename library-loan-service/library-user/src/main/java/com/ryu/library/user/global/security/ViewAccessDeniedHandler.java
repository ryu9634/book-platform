package com.ryu.library.user.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

/**
 * 화면 요청이 거부됐을 때 처리합니다.
 * CSRF 토큰이 맞지 않는 경우는 대부분 세션이 끝난 뒤(앱 재시작, 세션 만료) 예전에 열어 둔 화면에서 제출한 경우이므로
 * 403 화면 대신 로그인 화면으로 보내 다시 시도하게 합니다. 그 밖의 권한 부족은 그대로 403 오류 화면을 보여 줍니다.
 */
public class ViewAccessDeniedHandler implements AccessDeniedHandler {

    private final String expiredUrl;
    private final AccessDeniedHandler delegate = new AccessDeniedHandlerImpl();

    public ViewAccessDeniedHandler(String loginUrl) {
        this.expiredUrl = loginUrl + "?expired";
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (accessDeniedException instanceof CsrfException) {
            response.sendRedirect(request.getContextPath() + expiredUrl);
            return;
        }
        delegate.handle(request, response, accessDeniedException);
    }
}
