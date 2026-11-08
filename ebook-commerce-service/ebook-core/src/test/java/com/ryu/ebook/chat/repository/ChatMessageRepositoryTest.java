package com.ryu.ebook.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.chat.domain.ChatMessage;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ChatMessageRepositoryTest {

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    ChatSessionRepository chatSessionRepository;

    @Test
    @DisplayName("다음 순번은 마지막 순번에서 이어지고, 대화 기억으로 쓸 최근 메시지만 잘라 가져온다")
    void findLastSeqNo_최근메시지() {
        // given
        ChatSession session = chatSessionRepository.save(ChatSession.open(1L));
        Long sessionId = session.getId();
        assertThat(chatMessageRepository.findLastSeqNo(sessionId)).isZero();
        chatMessageRepository.save(ChatMessage.question(sessionId, 1, "첫 질문"));
        chatMessageRepository.save(ChatMessage.answer(sessionId, 2, "첫 답변", TokenUsage.of(100, 20)));
        chatMessageRepository.save(ChatMessage.question(sessionId, 3, "둘째 질문"));
        chatMessageRepository.saveAndFlush(ChatMessage.answer(sessionId, 4, "둘째 답변", TokenUsage.of(150, 30)));

        // when, then
        assertThat(chatMessageRepository.findLastSeqNo(sessionId)).isEqualTo(4);
        assertThat(chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(sessionId))
                .extracting(ChatMessage::getContent).containsExactly("첫 질문", "첫 답변", "둘째 질문", "둘째 답변");
        assertThat(chatMessageRepository.findByChatSessionIdOrderBySeqNoDesc(sessionId, PageRequest.of(0, 2)))
                .extracting(ChatMessage::getContent).containsExactly("둘째 답변", "둘째 질문");
    }

    @Test
    @DisplayName("대화를 지우면 그 세션의 메시지만 지워진다")
    void deleteByChatSessionId_다른세션유지() {
        // given
        Long kept = chatSessionRepository.save(ChatSession.open(1L)).getId();
        Long removed = chatSessionRepository.save(ChatSession.open(1L)).getId();
        chatMessageRepository.save(ChatMessage.question(kept, 1, "남는 질문"));
        chatMessageRepository.saveAndFlush(ChatMessage.question(removed, 1, "지울 질문"));

        // when
        chatMessageRepository.deleteByChatSessionId(removed);

        // then
        assertThat(chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(removed)).isEmpty();
        assertThat(chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(kept)).hasSize(1);
    }
}
