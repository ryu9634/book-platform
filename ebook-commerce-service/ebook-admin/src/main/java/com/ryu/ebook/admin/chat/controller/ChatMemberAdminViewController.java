package com.ryu.ebook.admin.chat.controller;

import com.ryu.ebook.admin.chat.service.ChatMemberAdminService;
import com.ryu.ebook.admin.global.security.LoginAdmin;
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

// 계정별 챗봇 사용 여부와 일일 토큰 한도. 한도를 비워 두면 전역 기본값을 따릅니다.
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/chat-members")
public class ChatMemberAdminViewController {

    private final ChatMemberAdminService chatMemberAdminService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", chatMemberAdminService.search(keyword, pageable));
        model.addAttribute("keyword", keyword);
        return "admin/chat/member-list";
    }

    @PostMapping("/{memberId}")
    public String changeSetting(@AuthenticationPrincipal LoginAdmin loginAdmin, @PathVariable Long memberId,
            @RequestParam(defaultValue = "false") boolean chatEnabled,
            @RequestParam(required = false) Long dailyTokenLimit,
            @RequestParam(required = false) String keyword, RedirectAttributes redirectAttributes) {
        try {
            chatMemberAdminService.changeSetting(memberId, chatEnabled, dailyTokenLimit, loginAdmin.getMemberId());
            redirectAttributes.addFlashAttribute("message", "설정을 저장했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        return "redirect:/admin/chat-members";
    }
}
