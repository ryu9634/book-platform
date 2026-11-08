package com.ryu.ebook.user.chat.dto;

import com.ryu.ebook.chat.domain.ChatMessage;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;

// 대화 한 줄. 답변 메시지에는 그 답변에 제목이 나온 전자책이 함께 담깁니다(질문 메시지는 늘 비어 있음).
public record ChatMessageResponse(boolean fromUser, String content, LocalDateTime createdAt,
        List<EbookSummaryResponse> recommendedBooks) {

    public static ChatMessageResponse of(ChatMessage message, List<EbookSummaryResponse> recommendedBooks) {
        return new ChatMessageResponse(message.isQuestion(), message.getContent(), message.getCreatedAt(),
                recommendedBooks);
    }
}
