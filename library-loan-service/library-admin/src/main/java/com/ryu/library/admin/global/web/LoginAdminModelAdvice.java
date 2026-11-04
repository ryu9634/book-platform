package com.ryu.library.admin.global.web;

import com.ryu.library.admin.global.security.LoginAdmin;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 모든 관리자 화면의 헤더에서 로그인 이름을 보여 주도록 모델에 공통으로 넣습니다.
@ControllerAdvice(annotations = Controller.class)
public class LoginAdminModelAdvice {

    @ModelAttribute("loginName")
    public String loginName(@AuthenticationPrincipal LoginAdmin loginAdmin) {
        return loginAdmin == null ? null : loginAdmin.getName();
    }
}
