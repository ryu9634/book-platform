package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원별 대화 세션. 회원당 진행 중(ACTIVE)인 세션은 하나이고, 새 대화를 시작하면 이전 세션을 닫습니다.
// 세션 ID가 모델에 넘기는 대화 식별자(conversationId)가 됩니다.
@Getter
@Entity
@Table(name = "CHAT_SESSION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseTimeEntity {

    private static final int TITLE_MAX_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_SESSION_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_SESSION_SEQ_GENERATOR", sequenceName = "CHAT_SESSION_SEQ", allocationSize = 50)
    @Column(name = "CHAT_SESSION_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatSessionStatus status;

    @Column(nullable = false)
    private int messageCount;

    private LocalDateTime lastMessageAt;

    private ChatSession(Long memberId) {
        this.memberId = memberId;
        this.status = ChatSessionStatus.ACTIVE;
        this.messageCount = 0;
    }

    public static ChatSession open(Long memberId) {
        return new ChatSession(memberId);
    }

    public void close() {
        this.status = ChatSessionStatus.CLOSED;
    }

    // 질문 하나와 답변 하나가 오갈 때마다 호출합니다. 목록에 보여 줄 제목은 첫 질문에서 한 번만 정합니다.
    public void recordExchange(String question, LocalDateTime now) {
        if (title == null && question != null && !question.isBlank()) {
            String trimmed = question.trim();
            this.title = trimmed.length() > TITLE_MAX_LENGTH ? trimmed.substring(0, TITLE_MAX_LENGTH) : trimmed;
        }
        this.messageCount += 2;
        this.lastMessageAt = now;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isActive() {
        return status == ChatSessionStatus.ACTIVE;
    }
}
