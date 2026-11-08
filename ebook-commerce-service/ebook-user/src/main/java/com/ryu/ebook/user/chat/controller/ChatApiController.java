package com.ryu.ebook.user.chat.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.user.chat.dto.ChatAnswerResponse;
import com.ryu.ebook.user.chat.dto.ChatQuestionRequest;
import com.ryu.ebook.user.chat.dto.ChatRoomResponse;
import com.ryu.ebook.user.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 도서 추천 챗봇 API. 화면과 같은 서비스를 쓰므로 한도와 세션 규칙이 그대로 적용됩니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatApiController {

    private final ChatService chatService;

    @GetMapping
    public ApiResponse<ChatRoomResponse> room(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(chatService.room(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ChatAnswerResponse>> ask(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChatQuestionRequest request) {
        ChatAnswerResponse answer = chatService.ask(Long.valueOf(jwt.getSubject()), request.question());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(answer));
    }

    @PostMapping("/new")
    public ApiResponse<Void> startNew(@AuthenticationPrincipal Jwt jwt) {
        chatService.startNewConversation(Long.valueOf(jwt.getSubject()));
        return ApiResponse.ok();
    }
}
