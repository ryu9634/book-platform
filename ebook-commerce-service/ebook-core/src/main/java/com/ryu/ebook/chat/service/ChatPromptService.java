package com.ryu.ebook.chat.service;

import com.ryu.ebook.chat.domain.ChatPrompt;
import com.ryu.ebook.chat.domain.ChatPromptHistory;
import com.ryu.ebook.chat.repository.ChatPromptHistoryRepository;
import com.ryu.ebook.chat.repository.ChatPromptRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 앱은 프롬프트를 읽어 모델에 넘기고, 관리자 앱은 프롬프트를 고칩니다. 두 앱이 함께 쓰므로 core에 둡니다.
// 고칠 때 이전 본문을 이력으로 남기는 것이 이 서비스가 지키는 규칙입니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatPromptService {

    private final ChatPromptRepository chatPromptRepository;
    private final ChatPromptHistoryRepository chatPromptHistoryRepository;

    // 모델에 넘길 시스템 프롬프트. 꺼 둔 프롬프트로는 답하지 않습니다.
    public String activeContent(String promptKey) {
        ChatPrompt prompt = chatPromptRepository.findByPromptKey(promptKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PROMPT_NOT_FOUND));
        if (!prompt.isEnabled()) {
            throw new BusinessException(ErrorCode.CHATBOT_SUSPENDED);
        }
        return prompt.getContent();
    }

    public ChatPrompt get(Long promptId) {
        return chatPromptRepository.findById(promptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PROMPT_NOT_FOUND));
    }

    @Transactional
    public void changeContent(Long promptId, String content, Long adminId) {
        ChatPrompt prompt = get(promptId);
        if (prompt.getContent().equals(content == null ? null : content.trim())) {
            return;
        }
        chatPromptHistoryRepository.save(ChatPromptHistory.ofPrevious(prompt, adminId));
        prompt.changeContent(content, adminId);
        log.info("chat prompt content changed. promptId={}, adminId={}", promptId, adminId);
    }

    @Transactional
    public void changeEnabled(Long promptId, boolean enabled, Long adminId) {
        ChatPrompt prompt = get(promptId);
        prompt.changeEnabled(enabled, adminId);
        log.info("chat prompt enabled changed. promptId={}, enabled={}, adminId={}", promptId, enabled, adminId);
    }
}
