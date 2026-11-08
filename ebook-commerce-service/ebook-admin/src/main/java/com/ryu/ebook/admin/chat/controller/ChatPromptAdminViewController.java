package com.ryu.ebook.admin.chat.controller;

import com.ryu.ebook.admin.chat.dto.ChatPromptForm;
import com.ryu.ebook.admin.chat.service.ChatPromptAdminService;
import com.ryu.ebook.admin.global.security.LoginAdmin;
import com.ryu.ebook.chat.service.ChatPromptService;
import com.ryu.ebook.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 챗봇 시스템 프롬프트 관리. 고치면 이전 본문이 이력에 남아 되돌릴 수 있습니다.
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/chat-prompts")
public class ChatPromptAdminViewController {

    private final ChatPromptAdminService chatPromptAdminService;
    private final ChatPromptService chatPromptService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("prompts", chatPromptAdminService.findAll());
        return "admin/chat/prompt-list";
    }

    @GetMapping("/{promptId}")
    public String editForm(@PathVariable Long promptId,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        return renderForm(promptId, chatPromptAdminService.form(promptId), pageable, model);
    }

    @PostMapping("/{promptId}")
    public String edit(@AuthenticationPrincipal LoginAdmin loginAdmin, @PathVariable Long promptId,
            @Valid @ModelAttribute("chatPromptForm") ChatPromptForm chatPromptForm, BindingResult bindingResult,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderForm(promptId, chatPromptForm, pageable, model);
        }
        try {
            chatPromptService.changeContent(promptId, chatPromptForm.getContent(), loginAdmin.getMemberId());
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(promptId, chatPromptForm, pageable, model);
        }
        redirectAttributes.addFlashAttribute("message", "프롬프트를 저장했습니다.");
        return "redirect:/admin/chat-prompts/" + promptId;
    }

    @PostMapping("/{promptId}/enabled")
    public String changeEnabled(@AuthenticationPrincipal LoginAdmin loginAdmin, @PathVariable Long promptId,
            @RequestParam boolean enabled, RedirectAttributes redirectAttributes) {
        chatPromptService.changeEnabled(promptId, enabled, loginAdmin.getMemberId());
        redirectAttributes.addFlashAttribute("message", enabled ? "프롬프트를 사용합니다." : "프롬프트를 중단했습니다.");
        return "redirect:/admin/chat-prompts";
    }

    private String renderForm(Long promptId, ChatPromptForm form, Pageable pageable, Model model) {
        model.addAttribute("chatPromptForm", form);
        model.addAttribute("prompt", chatPromptAdminService.get(promptId));
        model.addAttribute("historyPage", chatPromptAdminService.histories(promptId, pageable));
        return "admin/chat/prompt-form";
    }
}
