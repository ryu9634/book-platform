package com.ryu.ebook.user.cart.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.user.cart.service.CartService;
import com.ryu.ebook.user.global.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartViewController {

    private final CartService cartService;

    @GetMapping("/cart")
    public String cart(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("cart", cartService.get(loginMember.getMemberId()));
        return "cart/cart";
    }

    @PostMapping("/cart/items")
    public String add(@AuthenticationPrincipal LoginMember loginMember, @RequestParam Long ebookId,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.add(loginMember.getMemberId(), ebookId);
            redirectAttributes.addFlashAttribute("message", "장바구니에 담았습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // 같은 전자책을 동시에 두 번 담으면 (CART_ID, EBOOK_ID) UNIQUE 제약이 막습니다.
            redirectAttributes.addFlashAttribute("errorMessage", "이미 장바구니에 있는 전자책입니다.");
        }
        return "redirect:/ebooks/" + ebookId;
    }

    @PostMapping("/cart/items/{cartItemId}/delete")
    public String remove(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long cartItemId,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.remove(loginMember.getMemberId(), cartItemId);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }
}
