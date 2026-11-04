package com.ryu.library.user.loan.controller;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.service.LoanService;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.loan.service.LoanQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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
public class LoanViewController {

    private final LoanService loanService;
    private final LoanQueryService loanQueryService;

    // 대출 조건을 못 맞추거나 동시에 같은 권이 대출되면 오류 화면 대신 도서 상세로 돌아가 이유를 보여 줍니다.
    @PostMapping("/loans")
    public String loan(@AuthenticationPrincipal LoginMember loginMember, @RequestParam Long bookId,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.loanBook(loginMember.getMemberId(), bookId);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/" + bookId;
        } catch (OptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("errorMessage", ErrorCode.CONCURRENT_MODIFICATION.getMessage());
            return "redirect:/books/" + bookId;
        }
        redirectAttributes.addFlashAttribute("message", "대출했습니다. 반납 예정일을 확인해 주세요.");
        return "redirect:/loans/my";
    }

    @PostMapping("/loans/{loanId}/return")
    public String returnLoan(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long loanId,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.returnByMember(loginMember.getMemberId(), loanId);
            redirectAttributes.addFlashAttribute("message", "반납했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/loans/my";
    }

    @PostMapping("/loans/{loanId}/extend")
    public String extend(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long loanId,
            RedirectAttributes redirectAttributes) {
        try {
            loanService.extend(loginMember.getMemberId(), loanId);
            redirectAttributes.addFlashAttribute("message", "반납 예정일을 연장했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/loans/my";
    }

    @GetMapping("/loans/my")
    public String myLoans(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("loans", loanQueryService.myActiveLoans(loginMember.getMemberId()));
        return "loan/my";
    }

    @GetMapping("/loans/my/history")
    public String history(@AuthenticationPrincipal LoginMember loginMember,
            @PageableDefault(sort = "loanedAt", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", loanQueryService.myHistory(loginMember.getMemberId(), pageable));
        return "loan/history";
    }
}
