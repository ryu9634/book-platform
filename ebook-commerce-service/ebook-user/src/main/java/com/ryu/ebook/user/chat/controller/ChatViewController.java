package com.ryu.ebook.user.chat.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.user.chat.dto.ChatQuestionForm;
import com.ryu.ebook.user.chat.service.ChatService;
import com.ryu.ebook.user.global.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// AI 도서 추천 챗봇 화면. 질문을 보내면 PRG로 돌아와 대화를 다시 그립니다.
@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {

    private final ChatService chatService;

    @GetMapping
    public String room(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("room", chatService.room(loginMember.getMemberId()));
        if (!model.containsAttribute("chatQuestionForm")) {
            model.addAttribute("chatQuestionForm", new ChatQuestionForm());
        }
        return "chat/chat";
    }

    @PostMapping("/messages")
    public String ask(@AuthenticationPrincipal LoginMember loginMember,
            @Valid @ModelAttribute("chatQuestionForm") ChatQuestionForm chatQuestionForm, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("room", chatService.room(loginMember.getMemberId()));
            return "chat/chat";
        }
        try {
            // 답변과 추천 도서는 저장되므로 화면은 리다이렉트 후 대화를 다시 읽어 그립니다.
            chatService.ask(loginMember.getMemberId(), chatQuestionForm.getQuestion());
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/chat";
    }

    @PostMapping("/new")
    public String startNew(@AuthenticationPrincipal LoginMember loginMember,
            RedirectAttributes redirectAttributes) {
        chatService.startNewConversation(loginMember.getMemberId());
        redirectAttributes.addFlashAttribute("message", "새 대화를 시작합니다.");
        return "redirect:/chat";
    }
}
