package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.domain.ChatPromptHistory;
import java.time.LocalDateTime;

public record ChatPromptHistoryResponse(Long id, String content, Long changedBy, LocalDateTime createdAt) {

    public static ChatPromptHistoryResponse from(ChatPromptHistory history) {
        return new ChatPromptHistoryResponse(history.getId(), history.getContent(), history.getChangedBy(),
                history.getCreatedAt());
    }
}
