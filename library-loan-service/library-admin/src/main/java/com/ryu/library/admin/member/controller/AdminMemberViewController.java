package com.ryu.library.admin.member.controller;

import com.ryu.library.admin.global.security.LoginAdmin;
import com.ryu.library.admin.member.service.AdminMemberService;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.member.domain.MemberRole;
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

    @PostMapping("/{memberId}/suspend")
    public String suspend(@PathVariable Long memberId, @RequestParam int days, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "정지했습니다.", () -> adminMemberService.suspend(memberId, days));
    }

    @PostMapping("/{memberId}/release")
    public String release(@PathVariable Long memberId, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "정지를 해제했습니다.", () -> adminMemberService.releaseSuspension(memberId));
    }

    @PostMapping("/{memberId}/role")
    public String changeRole(@AuthenticationPrincipal LoginAdmin loginAdmin, @PathVariable Long memberId,
            @RequestParam MemberRole role, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "권한을 바꿨습니다.",
                () -> adminMemberService.changeRole(loginAdmin.getMemberId(), memberId, role));
    }

    private String run(RedirectAttributes redirectAttributes, String successMessage, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/members";
    }
}
