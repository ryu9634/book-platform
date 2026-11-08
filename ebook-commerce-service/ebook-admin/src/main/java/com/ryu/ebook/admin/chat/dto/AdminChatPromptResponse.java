package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.domain.ChatPrompt;
import java.time.LocalDateTime;

public record AdminChatPromptResponse(Long id, String promptKey, String name, String content, boolean enabled,
        Long updatedBy, LocalDateTime updatedAt) {

    public static AdminChatPromptResponse from(ChatPrompt prompt) {
        return new AdminChatPromptResponse(prompt.getId(), prompt.getPromptKey(), prompt.getName(),
                prompt.getContent(), prompt.isEnabled(), prompt.getUpdatedBy(), prompt.getUpdatedAt());
    }

    public String statusLabel() {
        return enabled ? "사용" : "중단";
    }
}
