package com.ryu.ebook.user.order.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.order.service.OrderService;
import com.ryu.ebook.order.service.RefundService;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.service.PaymentService;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.order.dto.OrderDetailResponse;
import com.ryu.ebook.user.order.service.OrderQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
public class OrderViewController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    @GetMapping("/orders/checkout")
    public String checkout(@AuthenticationPrincipal LoginMember loginMember,
            @RequestParam(required = false) List<Long> ebookIds, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("checkout", orderQueryService.checkout(loginMember.getMemberId(), ebookIds == null ? List.of() : ebookIds));
            model.addAttribute("methods", PaymentMethod.values());
            return "order/checkout";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/orders")
    public String create(@AuthenticationPrincipal LoginMember loginMember, @RequestParam List<Long> ebookIds,
            @RequestParam(defaultValue = "MOCK") PaymentMethod method, RedirectAttributes redirectAttributes) {
        try {
            String orderNo = orderService.create(loginMember.getMemberId(), ebookIds, method);
            return "redirect:/orders/" + orderNo + "/payment";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders/{orderNo}/payment")
    public String paymentPage(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo, Model model) {
        OrderDetailResponse order = orderQueryService.myOrder(loginMember.getMemberId(), orderNo);
        if (!order.pending()) {
            return "redirect:/orders/" + orderNo;
        }
        model.addAttribute("order", order);
        return "order/payment";
    }

    // 승인되면 완료 화면으로, 실패하면 결제 화면으로 돌아가 사유를 보여 줍니다(주문은 결제 대기로 남음).
    @PostMapping("/orders/{orderNo}/payment/approve")
    public String approve(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo,
            @RequestParam(defaultValue = "false") boolean simulateFailure, RedirectAttributes redirectAttributes) {
        try {
            PaymentService.PaymentOutcome outcome = paymentService.approve(loginMember.getMemberId(), orderNo, simulateFailure);
            if (outcome.approved()) {
                return "redirect:/orders/" + orderNo + "/complete";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "결제에 실패했습니다. " + outcome.failReason());
            return "redirect:/orders/" + orderNo + "/payment";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/orders/" + orderNo;
        }
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public String cancel(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(loginMember.getMemberId(), orderNo);
            redirectAttributes.addFlashAttribute("message", "주문을 취소했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + orderNo;
    }

    @PostMapping("/orders/{orderNo}/items/{orderItemId}/refund")
    public String refund(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo,
            @PathVariable Long orderItemId, RedirectAttributes redirectAttributes) {
        try {
            refundService.refundByMember(loginMember.getMemberId(), orderNo, orderItemId);
            redirectAttributes.addFlashAttribute("message", "환불했습니다. 내 서재에서 빠집니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + orderNo;
    }

    @GetMapping("/orders/{orderNo}/complete")
    public String complete(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo, Model model) {
        model.addAttribute("order", orderQueryService.myOrder(loginMember.getMemberId(), orderNo));
        return "order/complete";
    }

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal LoginMember loginMember,
            @PageableDefault(sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", orderQueryService.myOrders(loginMember.getMemberId(), pageable));
        return "order/list";
    }

    @GetMapping("/orders/{orderNo}")
    public String detail(@AuthenticationPrincipal LoginMember loginMember, @PathVariable String orderNo, Model model) {
        model.addAttribute("order", orderQueryService.myOrder(loginMember.getMemberId(), orderNo));
        return "order/detail";
    }
}
