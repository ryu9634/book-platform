package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 프롬프트를 고칠 때마다 바뀌기 전 본문을 남깁니다. 추천 품질이 나빠졌을 때 되돌릴 문장을 찾는 용도입니다.
@Getter
@Entity
@Table(name = "CHAT_PROMPT_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatPromptHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_PROMPT_HISTORY_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_PROMPT_HISTORY_SEQ_GENERATOR", sequenceName = "CHAT_PROMPT_HISTORY_SEQ",
            allocationSize = 50)
    @Column(name = "CHAT_PROMPT_HISTORY_ID")
    private Long id;

    @Column(nullable = false)
    private Long chatPromptId;

    @Lob
    @Column(nullable = false)
    private String content;

    // 논리 참조: MEMBER
    private Long changedBy;

    private ChatPromptHistory(Long chatPromptId, String content, Long changedBy) {
        this.chatPromptId = chatPromptId;
        this.content = content;
        this.changedBy = changedBy;
    }

    public static ChatPromptHistory ofPrevious(ChatPrompt prompt, Long changedBy) {
        return new ChatPromptHistory(prompt.getId(), prompt.getContent(), changedBy);
    }
}
