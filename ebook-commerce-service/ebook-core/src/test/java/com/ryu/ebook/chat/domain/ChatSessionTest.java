package com.ryu.ebook.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatSessionTest {

    @Test
    @DisplayName("세션 제목은 첫 질문에서 한 번만 정해지고, 주고받을 때마다 메시지 수가 둘씩 늘어난다")
    void recordExchange_여러번_제목고정() {
        // given
        ChatSession session = ChatSession.open(1L);
        LocalDateTime now = LocalDateTime.of(2026, 11, 8, 10, 0);

        // when
        session.recordExchange("쉬운 소설 추천해 줘", now);
        session.recordExchange("더 짧은 책은?", now.plusMinutes(1));

        // then
        assertThat(session.getTitle()).isEqualTo("쉬운 소설 추천해 줘");
        assertThat(session.getMessageCount()).isEqualTo(4);
        assertThat(session.getLastMessageAt()).isEqualTo(now.plusMinutes(1));
        assertThat(session.isActive()).isTrue();
    }

    @Test
    @DisplayName("제목이 길면 60자로 자르고, 닫은 세션은 더 이상 진행 중이 아니다")
    void recordExchange_긴질문_제목자름() {
        // given
        ChatSession session = ChatSession.open(1L);
        String longQuestion = "가".repeat(80);

        // when
        session.recordExchange(longQuestion, LocalDateTime.of(2026, 11, 8, 10, 0));
        session.close();

        // then
        assertThat(session.getTitle()).hasSize(60);
        assertThat(session.isActive()).isFalse();
        assertThat(session.isOwnedBy(1L)).isTrue();
        assertThat(session.isOwnedBy(2L)).isFalse();
    }
}
