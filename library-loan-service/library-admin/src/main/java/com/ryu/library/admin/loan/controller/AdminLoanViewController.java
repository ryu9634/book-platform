package com.ryu.library.admin.loan.controller;

import com.ryu.library.admin.loan.service.AdminLoanService;
import com.ryu.library.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/admin/loans")
public class AdminLoanViewController {

    private final AdminLoanService adminLoanService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "ACTIVE") String filter, @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "loanedAt", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", adminLoanService.search(filter, keyword, pageable));
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("today", adminLoanService.today());
        return "admin/loan/list";
    }

    @GetMapping("/overdue")
    public String overdue(Model model) {
        model.addAttribute("loans", adminLoanService.overdue());
        model.addAttribute("today", adminLoanService.today());
        return "admin/loan/overdue";
    }

    @PostMapping("/{loanId}/return")
    public String returnByDesk(@PathVariable Long loanId, @RequestParam(defaultValue = "/admin/loans") String back,
            RedirectAttributes redirectAttributes) {
        try {
            adminLoanService.returnByDesk(loanId);
            redirectAttributes.addFlashAttribute("message", "반납 처리했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        // 다른 사이트로 보내는 열린 리다이렉트를 막기 위해 관리자 대출 화면 경로만 허용합니다.
        return "redirect:" + (back.startsWith("/admin/loans") ? back : "/admin/loans");
    }
}
