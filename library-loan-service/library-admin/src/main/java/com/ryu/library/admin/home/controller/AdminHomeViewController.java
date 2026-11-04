package com.ryu.library.admin.home.controller;

import com.ryu.library.admin.home.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminHomeViewController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String root() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String home(Model model) {
        model.addAttribute("summary", dashboardService.summary());
        return "admin/home";
    }

    // 로그인 처리(POST /admin/login)는 Spring Security 필터가 맡고, 여기서는 화면만 보여 줍니다.
    @GetMapping("/admin/login")
    public String loginForm() {
        return "admin/login";
    }
}
