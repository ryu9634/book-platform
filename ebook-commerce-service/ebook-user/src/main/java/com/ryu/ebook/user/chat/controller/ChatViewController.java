package com.ryu.ebook.user.chat.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.chat.dto.ChatQuestionForm;
import com.ryu.ebook.user.chat.service.ChatService;
import com.ryu.ebook.user.global.security.LoginMember;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

// AI 도서 추천 챗봇 화면.
// 질문 보내기는 두 길이 있습니다. 폼 전송(/chat/messages)은 PRG로 돌아와 대화를 다시 그리고,
// 스트리밍(/chat/stream)은 답변을 글자 단위로 흘려 보냅니다. 화면의 자바스크립트가 있으면 스트리밍을 쓰고,
// 없거나 꺼 두면 폼 전송으로 그대로 동작합니다.
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatViewController {

    // SSE 이벤트 데이터를 UTF-8로 쓰기 위한 미디어 타입.
    private static final MediaType UTF8_TEXT = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private final ChatService chatService;
    private final ChatProperties chatProperties;
    private final Executor chatStreamExecutor;
    private final JsonMapper jsonMapper;

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

    // 답변을 글자 단위로 흘려 보냅니다. GET이 아니라 POST인 이유는 질문이 URL에 남지 않게 하려는 것입니다
    // (접속 로그에 회원이 쓴 문장이 그대로 남습니다). 그래서 EventSource 대신 화면에서 fetch로 읽습니다.
    // charset을 붙여 두는 이유: 이벤트 스트림은 UTF-8이 규약이지만 응답에 적어 두지 않으면
    // 중간 프록시나 테스트 도구가 ISO-8859-1로 읽어 한글이 깨집니다.
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter stream(@AuthenticationPrincipal LoginMember loginMember, @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(chatProperties.streamTimeout().toMillis());
        Long memberId = loginMember.getMemberId();
        chatStreamExecutor.execute(() -> {
            try {
                chatService.askStreaming(memberId, question, chunk -> send(emitter, "chunk", chunk));
                // 저장된 대화와 추천 도서는 화면이 다시 읽어 그립니다.
                send(emitter, "done", "");
            } catch (BusinessException e) {
                send(emitter, "error", e.getMessage());
            } catch (RuntimeException e) {
                log.warn("chat stream failed. memberId={}, cause={}", memberId, e.getClass().getSimpleName());
                send(emitter, "error", ErrorCode.CHAT_ANSWER_FAILED.getMessage());
            }
            emitter.complete();
        });
        return emitter;
    }

    // 조각을 JSON으로 감싸 보냅니다. SSE는 줄바꿈으로 필드를 나누므로 답변에 줄바꿈이 있으면 그대로는 깨집니다.
    // 미디어 타입에 charset을 직접 붙입니다. SseEmitter는 데이터를 text/plain으로 쓰고
    // Spring의 문자열 변환기 기본 charset은 ISO-8859-1이라, 지정하지 않으면 한글이 깨져 나갑니다.
    private void send(SseEmitter emitter, String name, String text) {
        try {
            emitter.send(SseEmitter.event().name(name)
                    .data(jsonMapper.writeValueAsString(Map.of("text", text)), UTF8_TEXT));
        } catch (IOException e) {
            // 회원이 화면을 닫으면 보낼 곳이 없습니다. 오류로 볼 일이 아니라 그냥 멈춥니다.
            log.debug("chat stream client gone. cause={}", e.getClass().getSimpleName());
        }
    }

    @PostMapping("/new")
    public String startNew(@AuthenticationPrincipal LoginMember loginMember,
            RedirectAttributes redirectAttributes) {
        chatService.startNewConversation(loginMember.getMemberId());
        redirectAttributes.addFlashAttribute("message", "새 대화를 시작합니다.");
        return "redirect:/chat";
    }
}
