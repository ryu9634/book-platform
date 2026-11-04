package com.ryu.ebook.user.ebook.controller;

import com.ryu.ebook.user.ebook.dto.EbookSort;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.review.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class EbookViewController {

    private final EbookQueryService ebookQueryService;
    private final ReviewService reviewService;

    @GetMapping("/ebooks")
    public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "POPULAR") EbookSort sort, Pageable pageable, Model model) {
        model.addAttribute("page", ebookQueryService.search(keyword, categoryId, sort, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", ebookQueryService.categoryOptions());
        return "ebook/list";
    }

    @GetMapping("/ebooks/{ebookId}")
    public String detail(@PathVariable Long ebookId, @AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("ebook", ebookQueryService.getEbook(ebookId));
        model.addAttribute("reviewSummary", reviewService.summary(ebookId));
        model.addAttribute("reviews", reviewService.reviews(ebookId,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        if (loginMember != null) {
            model.addAttribute("purchase", ebookQueryService.purchaseState(loginMember.getMemberId(), ebookId).name());
            model.addAttribute("canWriteReview", reviewService.canWrite(loginMember.getMemberId(), ebookId));
            model.addAttribute("loginMemberId", loginMember.getMemberId());
        }
        return "ebook/detail";
    }
}
