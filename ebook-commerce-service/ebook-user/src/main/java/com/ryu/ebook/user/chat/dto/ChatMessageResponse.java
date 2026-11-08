package com.ryu.ebook.user.chat.dto;

import com.ryu.ebook.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(boolean fromUser, String content, LocalDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.isQuestion(), message.getContent(), message.getCreatedAt());
    }
}
