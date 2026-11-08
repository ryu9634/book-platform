package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 대화 메시지. 다음 요청에 다시 실어 보낼 대화 기억이면서 화면에 뿌리는 이력입니다.
// 토큰 수는 답변 메시지에만 채워집니다(그 호출이 쓴 양).
//
// 규칙(docs/01-rules.md 3-1절) 예외: 세션 없이 존재할 수 없는 자식이지만 객체 참조와 cascade를 쓰지 않고 ID로 참조합니다.
// 대화가 길어지면 메시지가 수백 건까지 늘고, 쓰는 쪽은 언제나 마지막 순번과 최근 N건뿐이어서
// 세션이 컬렉션으로 들고 있을 이유가 없습니다. 세션을 지울 때는 deleteByChatSessionId로 한 번에 지웁니다.
@Getter
@Entity
@Table(name = "CHAT_MESSAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_MESSAGE_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_MESSAGE_SEQ_GENERATOR", sequenceName = "CHAT_MESSAGE_SEQ", allocationSize = 50)
    @Column(name = "CHAT_MESSAGE_ID")
    private Long id;

    @Column(nullable = false)
    private Long chatSessionId;

    @Column(nullable = false)
    private int seqNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int inputTokens;

    @Column(nullable = false)
    private int outputTokens;

    private ChatMessage(Long chatSessionId, int seqNo, ChatRole role, String content, long inputTokens,
            long outputTokens) {
        this.chatSessionId = chatSessionId;
        this.seqNo = seqNo;
        this.role = role;
        this.content = content;
        this.inputTokens = (int) inputTokens;
        this.outputTokens = (int) outputTokens;
    }

    public static ChatMessage question(Long chatSessionId, int seqNo, String content) {
        return new ChatMessage(chatSessionId, seqNo, ChatRole.USER, content, 0, 0);
    }

    public static ChatMessage answer(Long chatSessionId, int seqNo, String content, TokenUsage usage) {
        return new ChatMessage(chatSessionId, seqNo, ChatRole.ASSISTANT, content,
                usage.inputTokens() + usage.cacheCreateTokens() + usage.cacheReadTokens(), usage.outputTokens());
    }

    public boolean isQuestion() {
        return role == ChatRole.USER;
    }
}
