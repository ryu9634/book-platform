package com.ryu.ebook.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    @DisplayName("답변 메시지의 입력 토큰에는 캐시 토큰까지 합쳐 담고, 질문 메시지는 토큰이 0이다")
    void answer_캐시토큰포함() {
        // given
        TokenUsage usage = new TokenUsage(100, 40, 10, 20);

        // when
        ChatMessage question = ChatMessage.question(1L, 1, "추천해 줘");
        ChatMessage answer = ChatMessage.answer(1L, 2, "이 책이 좋습니다", usage);

        // then
        assertThat(question.isQuestion()).isTrue();
        assertThat(question.getInputTokens()).isZero();
        assertThat(answer.isQuestion()).isFalse();
        assertThat(answer.getInputTokens()).isEqualTo(130);
        assertThat(answer.getOutputTokens()).isEqualTo(40);
        assertThat(usage.totalTokens()).isEqualTo(170);
    }
}
