package com.ryu.ebook.user.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.chat.domain.ChatMessage;
import com.ryu.ebook.chat.domain.ChatMessageEbook;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.chat.repository.ChatMessageEbookRepository;
import com.ryu.ebook.chat.repository.ChatMessageRepository;
import com.ryu.ebook.chat.repository.ChatSessionRepository;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.chat.dto.ChatMessageResponse;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChatSessionServiceTest {

    private static final Long SESSION_ID = 3L;

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMessageEbookRepository chatMessageEbookRepository = mock(ChatMessageEbookRepository.class);
    private final EbookQueryService ebookQueryService = mock(EbookQueryService.class);
    private final ChatProperties chatProperties =
            new ChatProperties("claude-haiku-5-5", "low", 1200, 10, 3, 10, Duration.ofSeconds(3), true,
                    Duration.ofSeconds(120));
    private final ChatSessionService chatSessionService = new ChatSessionService(chatSessionRepository,
            chatMessageRepository, chatMessageEbookRepository, ebookQueryService, chatProperties,
            Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneId.of("Asia/Seoul")));

    private ChatMessage withId(ChatMessage message, Long id) {
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private EbookSummaryResponse ebook(Long id, String title) {
        return new EbookSummaryResponse(id, title, "저자", null, 10000, 9000, 10, "소설", true);
    }

    @Test
    @DisplayName("질문과 답변을 순번대로 저장하고, 추천 도서는 답변 메시지에 순서대로 묶는다")
    void appendExchange_추천도서저장() {
        // given
        ChatSession session = ChatSession.open(1L);
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(chatMessageRepository.findLastSeqNo(SESSION_ID)).willReturn(2);
        given(chatMessageRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 99L));

        // when
        chatSessionService.appendExchange(SESSION_ID, "추천해 줘", "이 책이 좋습니다", TokenUsage.of(100, 20),
                List.of(10L, 20L));

        // then
        verify(chatMessageEbookRepository, org.mockito.Mockito.times(2)).save(any(ChatMessageEbook.class));
        assertThat(session.getMessageCount()).isEqualTo(2);
        assertThat(session.getTitle()).isEqualTo("추천해 줘");
    }

    @Test
    @DisplayName("대화 이력은 답변마다 추천 도서를 붙이고, 그 사이 숨김으로 바뀐 책은 빼고 보여 준다")
    void messages_추천도서붙임() {
        // given
        ChatMessage question = withId(ChatMessage.question(SESSION_ID, 1, "추천해 줘"), 1L);
        ChatMessage answer = withId(ChatMessage.answer(SESSION_ID, 2, "이 책이 좋습니다", TokenUsage.of(100, 20)), 2L);
        given(chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(SESSION_ID))
                .willReturn(List.of(question, answer));
        given(chatMessageEbookRepository.findByChatMessageIdInOrderBySortOrderAsc(anyList())).willReturn(List.of(
                ChatMessageEbook.of(2L, 10L, 0), ChatMessageEbook.of(2L, 20L, 1)));
        // 20번 책은 그 사이 숨김으로 바뀌어 조회되지 않습니다.
        given(ebookQueryService.summariesById(anyList())).willReturn(Map.of(10L, ebook(10L, "이펙티브 자바")));

        // when
        List<ChatMessageResponse> messages = chatSessionService.messages(SESSION_ID);

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().fromUser()).isTrue();
        assertThat(messages.getFirst().recommendedBooks()).isEmpty();
        assertThat(messages.getLast().recommendedBooks()).extracting(EbookSummaryResponse::title)
                .containsExactly("이펙티브 자바");
    }

    @Test
    @DisplayName("대화가 없으면 링크와 전자책을 조회하지 않는다")
    void messages_빈대화_조회안함() {
        // given
        given(chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(SESSION_ID)).willReturn(List.of());

        // when
        List<ChatMessageResponse> messages = chatSessionService.messages(SESSION_ID);

        // then
        assertThat(messages).isEmpty();
        org.mockito.BDDMockito.then(chatMessageEbookRepository).shouldHaveNoInteractions();
        org.mockito.BDDMockito.then(ebookQueryService).shouldHaveNoInteractions();
    }
}
