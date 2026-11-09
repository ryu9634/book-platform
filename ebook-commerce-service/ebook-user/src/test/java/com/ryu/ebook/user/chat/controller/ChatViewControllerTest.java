package com.ryu.ebook.user.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.chat.dto.ChatAnswerResponse;
import com.ryu.ebook.user.chat.dto.ChatRoomResponse;
import com.ryu.ebook.user.chat.service.ChatService;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.support.WebSecurityTest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ChatViewController.class)
@WebSecurityTest
@Import(ChatViewControllerTest.StreamTestConfig.class)
class ChatViewControllerTest {

    // 스트리밍을 같은 스레드에서 돌려 테스트가 응답을 기다리지 않게 합니다.
    @TestConfiguration
    static class StreamTestConfig {

        @Bean
        Executor chatStreamExecutor() {
            return Runnable::run;
        }

        @Bean
        ChatProperties chatProperties() {
            return new ChatProperties("claude-haiku-5-5", "low", 1200, 10, 3, 10, Duration.ofSeconds(3), true,
                    Duration.ofSeconds(120));
        }
    }

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
                ChatRoomResponse.of(new ChatQuota(true, true, 20_000, 0, 1_000_000, 0), List.of(), true));
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
    @DisplayName("스트리밍은 받은 조각을 차례로 보내고 마지막에 끝났다는 이벤트를 보낸다")
    void stream_조각전달() throws Exception {
        // given
        given(chatService.askStreaming(eq(5L), eq("자바 책 추천해 줘"), any())).willAnswer(invocation -> {
            Consumer<String> onChunk = invocation.getArgument(2);
            onChunk.accept("이펙티브 ");
            onChunk.accept("자바를 권합니다.");
            return new ChatAnswerResponse("이펙티브 자바를 권합니다.", List.of());
        });

        // when, then
        MvcResult result = mockMvc.perform(post("/chat/stream").with(user(loginMember)).with(csrf())
                        .param("question", "자바 책 추천해 줘"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        // 실제로 나간 바이트를 UTF-8로 읽습니다(응답이 어떤 charset을 보고하든 내용이 맞아야 합니다).
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(body).contains("event:chunk").contains("이펙티브 ").contains("event:done");
    }

    @Test
    @DisplayName("스트리밍 중 한도를 넘으면 이유를 error 이벤트로 보낸다")
    void stream_한도초과_에러이벤트() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED))
                .given(chatService).askStreaming(eq(5L), eq("추천해 줘"), any());

        // when, then
        MvcResult result = mockMvc.perform(post("/chat/stream").with(user(loginMember)).with(csrf())
                        .param("question", "추천해 줘"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains("event:error")
                .contains("오늘 쓸 수 있는 AI 추천 분량");
    }

    @Test
    @DisplayName("스트리밍도 CSRF 토큰이 없으면 받지 않는다")
    void stream_CSRF없음_거절() throws Exception {
        mockMvc.perform(post("/chat/stream").with(user(loginMember)).param("question", "추천해 줘"))
                .andExpect(redirectedUrl("/login?expired"));
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
