package com.ryu.ebook.user.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.chat.dto.ChatAnswerResponse;
import com.ryu.ebook.user.chat.dto.ChatRoomResponse;
import com.ryu.ebook.user.chat.service.ChatService;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.support.WebSecurityTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ChatApiController.class)
@WebSecurityTest
class ChatApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    private MockHttpServletRequestBuilder ask(String body) {
        return post("/api/v1/chat/messages")
                .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("대화 조회는 남은 사용량과 지난 대화를 공통 응답 형식으로 돌려준다")
    void room_공통응답() throws Exception {
        // given
        given(chatService.room(5L)).willReturn(
                ChatRoomResponse.of(new ChatQuota(true, true, 20_000, 5_000, 1_000_000, 10_000), List.of(), true));

        // when, then
        mockMvc.perform(get("/api/v1/chat")
                        .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.remainingTokens").value(15000));
    }

    @Test
    @DisplayName("질문에 답하면 201과 함께 답변과 추천 전자책을 돌려준다")
    void ask_성공_201() throws Exception {
        // given
        given(chatService.ask(5L, "자바 책 추천해 줘")).willReturn(new ChatAnswerResponse("이펙티브 자바를 권합니다.",
                List.of(new EbookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", null, 36000, 32400, 10,
                        "IT/프로그래밍", true))));

        // when, then
        mockMvc.perform(ask("{\"question\":\"자바 책 추천해 줘\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.answer").value("이펙티브 자바를 권합니다."))
                .andExpect(jsonPath("$.data.recommendedBooks[0].title").value("이펙티브 자바"));
    }

    @Test
    @DisplayName("빈 질문은 400으로 막고 모델을 부르지 않는다")
    void ask_빈질문_400() throws Exception {
        mockMvc.perform(ask("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        verify(chatService, never()).ask(anyLong(), any());
    }

    @Test
    @DisplayName("한도를 넘으면 429, 모델이 거절하면 400을 공통 에러 형식으로 돌려준다")
    void ask_한도초과_429() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED)).given(chatService).ask(5L, "한도 초과");
        willThrow(new BusinessException(ErrorCode.CHAT_ANSWER_REJECTED)).given(chatService).ask(5L, "이상한 질문");

        // when, then
        mockMvc.perform(ask("{\"question\":\"한도 초과\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("CB003"));
        mockMvc.perform(ask("{\"question\":\"이상한 질문\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CB010"));
    }

    @Test
    @DisplayName("토큰 없이 부르면 401이고, 새 대화 요청은 지금 대화를 닫는다")
    void startNew_인증필요() throws Exception {
        mockMvc.perform(post("/api/v1/chat/new")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/chat/new")
                        .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
        verify(chatService).startNewConversation(5L);
    }
}
