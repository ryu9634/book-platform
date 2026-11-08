package com.ryu.ebook.user.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.user.chat.dto.ChatAnswerResponse;
import com.ryu.ebook.user.chat.dto.ChatRoomResponse;
import com.ryu.ebook.user.chat.service.ChatService;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.support.WebSecurityTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatViewController.class)
@WebSecurityTest
class ChatViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        loginMember = LoginMember.from(MemberFixture.activeMember());
        ReflectionTestUtils.setField(loginMember, "memberId", 5L);
        given(chatService.room(5L)).willReturn(
                ChatRoomResponse.of(new ChatQuota(true, true, 20_000, 0, 1_000_000, 0), List.of()));
    }

    @Test
    @DisplayName("대화 화면은 로그인한 회원에게만 열리고, 비로그인은 로그인 화면으로 보낸다")
    void room_인증필요() throws Exception {
        mockMvc.perform(get("/chat").with(user(loginMember)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("room", "chatQuestionForm"));
        mockMvc.perform(get("/chat")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("질문에 답하면 대화 화면으로 돌아가고, 저장된 대화를 다시 읽어 그린다")
    void ask_성공_리다이렉트() throws Exception {
        // given
        given(chatService.ask(5L, "자바 책 추천해 줘")).willReturn(new ChatAnswerResponse("이펙티브 자바를 권합니다.",
                List.of(new EbookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", null, 36000, 32400, 10,
                        "IT/프로그래밍", true))));

        // when, then
        mockMvc.perform(post("/chat/messages").with(user(loginMember)).with(csrf())
                        .param("question", "자바 책 추천해 줘"))
                .andExpect(redirectedUrl("/chat"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @DisplayName("빈 질문은 모델을 부르지 않고 그 자리에서 오류를 보여 준다")
    void ask_빈질문_필드오류() throws Exception {
        mockMvc.perform(post("/chat/messages").with(user(loginMember)).with(csrf()).param("question", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("chatQuestionForm", "question"));
        verify(chatService, never()).ask(anyLong(), any());
    }

    @Test
    @DisplayName("한도를 넘었거나 답할 수 없는 질문이면 이유를 안내 메시지로 보여 준다")
    void ask_업무예외_안내메시지() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED))
                .given(chatService).ask(5L, "추천해 줘");

        // when, then
        mockMvc.perform(post("/chat/messages").with(user(loginMember)).with(csrf()).param("question", "추천해 줘"))
                .andExpect(redirectedUrl("/chat"))
                .andExpect(flash().attribute("errorMessage",
                        ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED.getMessage()));
    }

    @Test
    @DisplayName("새 대화를 누르면 지금 대화를 닫고 돌아오며, CSRF 토큰이 없는 요청은 받지 않는다")
    void startNew_세션닫기() throws Exception {
        mockMvc.perform(post("/chat/new").with(user(loginMember)).with(csrf()))
                .andExpect(redirectedUrl("/chat"))
                .andExpect(flash().attribute("message", "새 대화를 시작합니다."));
        verify(chatService).startNewConversation(5L);

        mockMvc.perform(post("/chat/new").with(user(loginMember)))
                .andExpect(redirectedUrl("/login?expired"));
    }
}
