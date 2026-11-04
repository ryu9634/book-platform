package com.ryu.ebook.user.bookshelf.controller;

import com.ryu.ebook.bookshelf.service.ReadingService;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import com.ryu.ebook.user.global.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BookshelfViewController {

    private final BookshelfQueryService bookshelfQueryService;
    private final ReadingService readingService;

    @GetMapping("/bookshelf")
    public String bookshelf(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("books", bookshelfQueryService.myBookshelf(loginMember.getMemberId()));
        return "bookshelf/list";
    }

    // page를 비우면 마지막으로 읽은 쪽부터 엽니다(이어 읽기).
    @GetMapping("/bookshelf/{bookshelfId}/read")
    public String read(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long bookshelfId,
            @RequestParam(defaultValue = "0") int page, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("reading", readingService.read(loginMember.getMemberId(), bookshelfId, page));
            return "bookshelf/viewer";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookshelf";
        }
    }
}
