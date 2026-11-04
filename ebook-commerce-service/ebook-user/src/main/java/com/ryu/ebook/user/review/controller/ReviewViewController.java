package com.ryu.ebook.user.review.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.review.domain.Review;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.review.dto.ReviewForm;
import com.ryu.ebook.user.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewViewController {

    private final ReviewService reviewService;
    private final EbookQueryService ebookQueryService;

    @GetMapping("/ebooks/{ebookId}/reviews/new")
    public String writeForm(@PathVariable Long ebookId, Model model) {
        return renderForm(ebookId, null, new ReviewForm(), model);
    }

    @PostMapping("/ebooks/{ebookId}/reviews")
    public String write(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long ebookId,
            @Valid @ModelAttribute("reviewForm") ReviewForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderForm(ebookId, null, form, model);
        }
        try {
            reviewService.write(loginMember.getMemberId(), ebookId, form.getRating(), form.getContent());
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(ebookId, null, form, model);
        } catch (DataIntegrityViolationException e) {
            bindingResult.reject("RV002", "이미 리뷰를 쓴 전자책입니다. 기존 리뷰를 수정해 주세요.");
            return renderForm(ebookId, null, form, model);
        }
        redirectAttributes.addFlashAttribute("message", "리뷰를 등록했습니다.");
        return "redirect:/ebooks/" + ebookId;
    }

    @GetMapping("/reviews/{reviewId}/edit")
    public String editForm(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long reviewId, Model model) {
        Review review = reviewService.get(loginMember.getMemberId(), reviewId);
        return renderForm(review.getEbookId(), reviewId, ReviewForm.of(review.getRating(), review.getContent()), model);
    }

    @PostMapping("/reviews/{reviewId}/edit")
    public String edit(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long reviewId,
            @Valid @ModelAttribute("reviewForm") ReviewForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        Review review = reviewService.get(loginMember.getMemberId(), reviewId);
        if (bindingResult.hasErrors()) {
            return renderForm(review.getEbookId(), reviewId, form, model);
        }
        Long ebookId = reviewService.edit(loginMember.getMemberId(), reviewId, form.getRating(), form.getContent());
        redirectAttributes.addFlashAttribute("message", "리뷰를 수정했습니다.");
        return "redirect:/ebooks/" + ebookId;
    }

    @PostMapping("/reviews/{reviewId}/delete")
    public String delete(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long reviewId,
            RedirectAttributes redirectAttributes) {
        Long ebookId = reviewService.delete(loginMember.getMemberId(), reviewId);
        redirectAttributes.addFlashAttribute("message", "리뷰를 삭제했습니다.");
        return "redirect:/ebooks/" + ebookId;
    }

    private String renderForm(Long ebookId, Long reviewId, ReviewForm form, Model model) {
        model.addAttribute("ebook", ebookQueryService.getEbook(ebookId));
        model.addAttribute("reviewId", reviewId);
        model.addAttribute("reviewForm", form);
        return "review/form";
    }
}
