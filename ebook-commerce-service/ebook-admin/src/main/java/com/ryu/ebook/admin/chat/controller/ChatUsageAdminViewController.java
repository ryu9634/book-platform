package com.ryu.ebook.admin.chat.controller;

import com.ryu.ebook.admin.chat.dto.ChatSettingForm;
import com.ryu.ebook.admin.chat.service.ChatUsageAdminService;
import com.ryu.ebook.admin.global.security.LoginAdmin;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 날짜별 토큰 사용량과 추정 비용, 그리고 챗봇 전체를 멈추는 스위치.
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/chat-usage")
public class ChatUsageAdminViewController {

    private final ChatUsageAdminService chatUsageAdminService;

    @GetMapping
    public String list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable, Model model) {
        return render(date, pageable, chatUsageAdminService.settingForm(), model);
    }

    @PostMapping("/settings")
    public String changeSetting(@AuthenticationPrincipal LoginAdmin loginAdmin,
            @Valid @ModelAttribute("chatSettingForm") ChatSettingForm chatSettingForm, BindingResult bindingResult,
            @PageableDefault(size = 20) Pageable pageable, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return render(null, pageable, chatSettingForm, model);
        }
        chatUsageAdminService.changeSetting(chatSettingForm.isEnabled(), chatSettingForm.getDefaultDailyTokenLimit(),
                chatSettingForm.getGlobalDailyTokenLimit(), loginAdmin.getMemberId());
        redirectAttributes.addFlashAttribute("message",
                chatSettingForm.isEnabled() ? "설정을 저장했습니다." : "챗봇을 전체 중단했습니다.");
        return "redirect:/admin/chat-usage";
    }

    private String render(LocalDate date, Pageable pageable, ChatSettingForm form, Model model) {
        LocalDate target = date == null ? chatUsageAdminService.today() : date;
        model.addAttribute("date", target);
        model.addAttribute("page", chatUsageAdminService.usage(target, pageable));
        model.addAttribute("summary", chatUsageAdminService.summary(target));
        model.addAttribute("chatSettingForm", form);
        return "admin/chat/usage";
    }
}
