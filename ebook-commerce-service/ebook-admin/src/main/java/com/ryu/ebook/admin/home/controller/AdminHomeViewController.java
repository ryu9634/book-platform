package com.ryu.ebook.admin.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeViewController {

    @GetMapping("/")
    public String root() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String home() {
        return "admin/home";
    }

    // 로그인 처리(POST /admin/login)는 Spring Security 필터가 맡고, 여기서는 화면만 보여 줍니다.
    @GetMapping("/admin/login")
    public String loginForm() {
        return "admin/login";
    }
}
