package com.ryu.ebook.user.home.controller;

import com.ryu.ebook.user.ebook.service.EbookQueryService;
import com.ryu.ebook.user.library.service.LibraryRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeViewController {

    private final EbookQueryService ebookQueryService;
    private final LibraryRecommendationService libraryRecommendationService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bestsellers", ebookQueryService.bestsellers());
        model.addAttribute("newArrivals", ebookQueryService.newArrivals());
        model.addAttribute("fromLibrary", libraryRecommendationService.recommend());
        return "home";
    }
}
