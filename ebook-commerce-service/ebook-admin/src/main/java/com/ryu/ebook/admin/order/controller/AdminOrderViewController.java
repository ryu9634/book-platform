package com.ryu.ebook.admin.order.controller;

import com.ryu.ebook.admin.order.service.AdminOrderService;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.order.domain.OrderStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderViewController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", adminOrderService.search(status, from, to, keyword, pageable));
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("keyword", keyword);
        return "admin/order/list";
    }

    @GetMapping("/{orderNo}")
    public String detail(@PathVariable String orderNo, Model model) {
        model.addAttribute("detail", adminOrderService.get(orderNo));
        return "admin/order/detail";
    }

    @PostMapping("/{orderNo}/refund")
    public String refund(@PathVariable String orderNo, RedirectAttributes redirectAttributes) {
        try {
            adminOrderService.refundAll(orderNo);
            redirectAttributes.addFlashAttribute("message", "남은 항목을 모두 환불했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderNo;
    }
}
