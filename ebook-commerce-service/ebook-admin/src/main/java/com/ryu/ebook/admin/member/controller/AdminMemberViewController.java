package com.ryu.ebook.admin.member.controller;

import com.ryu.ebook.admin.global.security.LoginAdmin;
import com.ryu.ebook.admin.member.service.AdminMemberService;
import com.ryu.ebook.global.exception.BusinessException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberViewController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", adminMemberService.search(keyword, pageable));
        model.addAttribute("keyword", keyword);
        return "admin/member/list";
    }

    @PostMapping("/{memberId}/withdraw")
    public String withdraw(@AuthenticationPrincipal LoginAdmin loginAdmin, @PathVariable Long memberId,
            RedirectAttributes redirectAttributes) {
        try {
            adminMemberService.withdraw(loginAdmin.getMemberId(), memberId);
            redirectAttributes.addFlashAttribute("message", "탈퇴 처리했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "자기 자신은 탈퇴 처리할 수 없습니다.");
        }
        return "redirect:/admin/members";
    }
}
