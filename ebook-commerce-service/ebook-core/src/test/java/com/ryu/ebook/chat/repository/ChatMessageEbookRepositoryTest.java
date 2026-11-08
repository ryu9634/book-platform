package com.ryu.ebook.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.chat.domain.ChatMessage;
import com.ryu.ebook.chat.domain.ChatMessageEbook;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ChatMessageEbookRepositoryTest {

    @Autowired
    ChatMessageEbookRepository chatMessageEbookRepository;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    ChatSessionRepository chatSessionRepository;

    @Test
    @DisplayName("추천 도서는 답변에 나온 순서대로 가져오고, 여러 메시지를 한 번에 읽을 수 있다")
    void findByChatMessageIdIn_순서유지() {
        // given
        Long sessionId = chatSessionRepository.save(ChatSession.open(1L)).getId();
        ChatMessage first = chatMessageRepository.save(ChatMessage.answer(sessionId, 2, "첫 답변",
                TokenUsage.of(100, 20)));
        ChatMessage second = chatMessageRepository.save(ChatMessage.answer(sessionId, 4, "둘째 답변",
                TokenUsage.of(120, 30)));
        chatMessageEbookRepository.save(ChatMessageEbook.of(first.getId(), 20L, 1));
        chatMessageEbookRepository.save(ChatMessageEbook.of(first.getId(), 10L, 0));
        chatMessageEbookRepository.saveAndFlush(ChatMessageEbook.of(second.getId(), 30L, 0));

        // when
        List<ChatMessageEbook> links = chatMessageEbookRepository
                .findByChatMessageIdInOrderBySortOrderAsc(List.of(first.getId(), second.getId()));

        // then
        assertThat(links).hasSize(3);
        assertThat(links).filteredOn(link -> link.getChatMessageId().equals(first.getId()))
                .extracting(ChatMessageEbook::getEbookId).containsExactly(10L, 20L);
        assertThat(chatMessageEbookRepository.findByChatMessageIdInOrderBySortOrderAsc(List.of())).isEmpty();
    }

    @Test
    @DisplayName("같은 답변에 같은 전자책을 두 번 묶을 수 없다")
    void save_중복_제약위반() {
        // given
        Long sessionId = chatSessionRepository.save(ChatSession.open(1L)).getId();
        ChatMessage answer = chatMessageRepository.save(ChatMessage.answer(sessionId, 2, "답변",
                TokenUsage.of(100, 20)));
        chatMessageEbookRepository.saveAndFlush(ChatMessageEbook.of(answer.getId(), 10L, 0));

        // when, then
        assertThatThrownBy(() ->
                chatMessageEbookRepository.saveAndFlush(ChatMessageEbook.of(answer.getId(), 10L, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
