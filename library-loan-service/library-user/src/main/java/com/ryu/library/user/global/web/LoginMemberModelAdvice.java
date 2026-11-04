package com.ryu.library.user.global.web;

import com.ryu.library.user.global.security.LoginMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 모든 화면의 헤더에서 로그인 이름을 보여 주도록 화면 컨트롤러 모델에 공통으로 넣습니다.
@ControllerAdvice(annotations = Controller.class)
public class LoginMemberModelAdvice {

    @ModelAttribute("loginName")
    public String loginName(@AuthenticationPrincipal LoginMember loginMember) {
        return loginMember == null ? null : loginMember.getName();
    }
}
