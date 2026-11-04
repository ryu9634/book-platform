package com.ryu.ebook.user.bookshelf.controller;

import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import com.ryu.ebook.user.global.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BookshelfViewController {

    private final BookshelfQueryService bookshelfQueryService;

    @GetMapping("/bookshelf")
    public String bookshelf(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("books", bookshelfQueryService.myBookshelf(loginMember.getMemberId()));
        return "bookshelf/list";
    }
}
