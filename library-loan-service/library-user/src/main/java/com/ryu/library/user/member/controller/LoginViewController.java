package com.ryu.library.user.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 로그인 처리(POST /login)는 Spring Security 필터가 맡고, 여기서는 화면만 보여 줍니다.
@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}
