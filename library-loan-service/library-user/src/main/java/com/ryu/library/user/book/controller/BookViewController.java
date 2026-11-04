package com.ryu.library.user.book.controller;

import com.ryu.library.user.book.service.BookQueryService;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.loan.service.LoanQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class BookViewController {

    private final BookQueryService bookQueryService;
    private final LoanQueryService loanQueryService;

    @GetMapping("/books")
    public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long categoryId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", bookQueryService.search(keyword, categoryId, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", bookQueryService.categoryOptions());
        return "book/list";
    }

    @GetMapping("/books/{bookId}")
    public String detail(@PathVariable Long bookId, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("book", bookQueryService.getBook(bookId));
        if (loginMember != null) {
            model.addAttribute("action", loanQueryService.actionState(loginMember.getMemberId(), bookId));
        }
        return "book/detail";
    }
}
