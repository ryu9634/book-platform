package com.ryu.library.user.home.controller;

import com.ryu.library.user.book.service.BookQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeViewController {

    private final BookQueryService bookQueryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("newArrivals", bookQueryService.newArrivals());
        return "home";
    }
}
