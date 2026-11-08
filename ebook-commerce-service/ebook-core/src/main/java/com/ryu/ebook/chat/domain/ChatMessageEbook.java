package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 답변에 제목이 나온 전자책. 화면과 API가 같은 추천 결과를 보여 주도록 답변 메시지에 묶어 둡니다.
@Getter
@Entity
@Table(name = "CHAT_MESSAGE_EBOOK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEbook extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_MESSAGE_EBOOK_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_MESSAGE_EBOOK_SEQ_GENERATOR", sequenceName = "CHAT_MESSAGE_EBOOK_SEQ",
            allocationSize = 50)
    @Column(name = "CHAT_MESSAGE_EBOOK_ID")
    private Long id;

    @Column(nullable = false)
    private Long chatMessageId;

    @Column(nullable = false)
    private Long ebookId;

    @Column(nullable = false)
    private int sortOrder;

    private ChatMessageEbook(Long chatMessageId, Long ebookId, int sortOrder) {
        this.chatMessageId = chatMessageId;
        this.ebookId = ebookId;
        this.sortOrder = sortOrder;
    }

    public static ChatMessageEbook of(Long chatMessageId, Long ebookId, int sortOrder) {
        return new ChatMessageEbook(chatMessageId, ebookId, sortOrder);
    }
}
