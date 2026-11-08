package com.ryu.ebook.user.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.chat.domain.ChatPrompt;
import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.chat.service.ChatPromptService;
import com.ryu.ebook.chat.service.ChatQuotaService;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.chat.client.ChatCompletion;
import com.ryu.ebook.user.chat.client.ChatCompletionPort;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static final Long MEMBER_ID = 5L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-11-08T01:00:00Z");

    private final ChatQuotaService chatQuotaService = mock(ChatQuotaService.class);
    private final ChatPromptService chatPromptService = mock(ChatPromptService.class);
    private final ChatSessionService chatSessionService = mock(ChatSessionService.class);
    private final ChatCompletionPort chatCompletionPort = mock(ChatCompletionPort.class);
    private final ChatProperties chatProperties =
            new ChatProperties("claude-haiku-5-5", "low", 1200, 10, 3, 10, Duration.ofSeconds(3));
    private final ChatService chatService = new ChatService(chatQuotaService, chatPromptService, chatSessionService,
            chatCompletionPort, chatProperties, Clock.fixed(NOW, ZONE));

    private final EbookSummaryResponse recommended =
            new EbookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", null, 36000, 32400, 10, "IT/프로그래밍", true);
    private final EbookSummaryResponse notMentioned =
            new EbookSummaryResponse(2L, "클린 코드", "로버트 마틴", null, 30000, 30000, 0, "IT/프로그래밍", true);

    @Test
    @DisplayName("질문에 답하면 토큰을 기록하고 대화를 저장하며, 답변에 제목이 나온 책만 화면에 돌려준다")
    void ask_정상_저장과추천() {
        // given
        ChatSession session = ChatSession.open(MEMBER_ID);
        given(chatSessionService.openIfAbsent(MEMBER_ID)).willReturn(session);
        given(chatPromptService.activeContent(ChatPrompt.BOOK_RECOMMEND)).willReturn("시스템 프롬프트");
        given(chatSessionService.recentTurns(any())).willReturn(List.of());
        TokenUsage usage = TokenUsage.of(1000, 200);
        given(chatCompletionPort.answer(eq("시스템 프롬프트"), any(), eq("자바 책 추천해 줘"), eq(MEMBER_ID)))
                .willReturn(new ChatCompletion("이펙티브 자바를 권합니다.", usage, false, false,
                        List.of(recommended, notMentioned)));

        // when
        var answer = chatService.ask(MEMBER_ID, "  자바 책 추천해 줘  ");

        // then
        assertThat(answer.answer()).isEqualTo("이펙티브 자바를 권합니다.");
        assertThat(answer.recommendedBooks()).extracting(EbookSummaryResponse::title).containsExactly("이펙티브 자바");
        verify(chatQuotaService).ensureAvailable(MEMBER_ID);
        verify(chatQuotaService).record(MEMBER_ID, usage);
        // 추천한 책은 답변 메시지에 묶여 저장됩니다(화면과 API가 같은 결과를 보여 주도록).
        verify(chatSessionService).appendExchange(any(), eq("자바 책 추천해 줘"), eq("이펙티브 자바를 권합니다."),
                eq(usage), eq(List.of(1L)));
    }

    @Test
    @DisplayName("한도를 넘은 계정은 모델을 부르지 않고 바로 막는다")
    void ask_한도초과_호출안함() {
        // given
        willThrow(new BusinessException(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED))
                .given(chatQuotaService).ensureAvailable(MEMBER_ID);

        // when, then
        assertThatThrownBy(() -> chatService.ask(MEMBER_ID, "추천해 줘"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED);
        then(chatCompletionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("직전 질문 뒤 너무 빨리 또 물으면 모델을 부르지 않는다")
    void ask_연타_호출안함() {
        // given
        ChatSession session = ChatSession.open(MEMBER_ID);
        session.recordExchange("직전 질문", LocalDateTime.now(Clock.fixed(NOW, ZONE)).minusSeconds(1));
        given(chatSessionService.openIfAbsent(MEMBER_ID)).willReturn(session);

        // when, then
        assertThatThrownBy(() -> chatService.ask(MEMBER_ID, "또 추천해 줘"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_TOO_MANY_REQUESTS);
        then(chatCompletionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("모델이 답변을 거절하면 대화는 저장하지 않지만 이미 쓴 토큰은 기록한다")
    void ask_거절_토큰만기록() {
        // given
        given(chatSessionService.openIfAbsent(MEMBER_ID)).willReturn(ChatSession.open(MEMBER_ID));
        given(chatPromptService.activeContent(anyString())).willReturn("시스템 프롬프트");
        given(chatSessionService.recentTurns(any())).willReturn(List.of());
        TokenUsage usage = TokenUsage.of(500, 0);
        given(chatCompletionPort.answer(any(), any(), any(), anyLong()))
                .willReturn(new ChatCompletion("", usage, true, false, List.of()));

        // when, then
        assertThatThrownBy(() -> chatService.ask(MEMBER_ID, "이상한 질문"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ANSWER_REJECTED);
        verify(chatQuotaService).record(MEMBER_ID, usage);
        verify(chatSessionService, never()).appendExchange(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("출력 상한에 걸려 끊긴 답변에는 끊겼다는 안내를 덧붙여 저장한다")
    void ask_출력상한_안내덧붙임() {
        // given
        given(chatSessionService.openIfAbsent(MEMBER_ID)).willReturn(ChatSession.open(MEMBER_ID));
        given(chatPromptService.activeContent(anyString())).willReturn("시스템 프롬프트");
        given(chatSessionService.recentTurns(any())).willReturn(List.of());
        given(chatCompletionPort.answer(any(), any(), any(), anyLong()))
                .willReturn(new ChatCompletion("추천은", TokenUsage.of(100, 1200), false, true, List.of()));

        // when
        chatService.ask(MEMBER_ID, "길게 설명해 줘");

        // then
        verify(chatSessionService).appendExchange(any(), eq("길게 설명해 줘"),
                org.mockito.ArgumentMatchers.contains("여기서 끊었습니다"), any(), any());
    }

    @Test
    @DisplayName("빈 질문이나 500자를 넘는 질문은 모델을 부르기 전에 막는다")
    void ask_질문길이_예외() {
        // given, when, then
        assertThatThrownBy(() -> chatService.ask(MEMBER_ID, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> chatService.ask(MEMBER_ID, "가".repeat(501)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        then(chatQuotaService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("대화 화면은 남은 사용량과 지난 대화를 함께 내려 준다")
    void room_사용량과대화() {
        // given
        given(chatQuotaService.quota(MEMBER_ID))
                .willReturn(new ChatQuota(true, true, 20_000, 5_000, 1_000_000, 10_000));
        given(chatSessionService.findActive(MEMBER_ID)).willReturn(Optional.empty());

        // when
        var room = chatService.room(MEMBER_ID);

        // then
        assertThat(room.available()).isTrue();
        assertThat(room.unavailableReason()).isNull();
        assertThat(room.remainingTokens()).isEqualTo(15_000);
        assertThat(room.remainingPercent()).isEqualTo(75);
        assertThat(room.messages()).isEmpty();
    }
}
