package com.ryu.ebook.admin.chat.service;

import com.ryu.ebook.admin.chat.dto.AdminChatPromptResponse;
import com.ryu.ebook.admin.chat.dto.ChatPromptForm;
import com.ryu.ebook.admin.chat.dto.ChatPromptHistoryResponse;
import com.ryu.ebook.chat.repository.ChatPromptHistoryRepository;
import com.ryu.ebook.chat.repository.ChatPromptRepository;
import com.ryu.ebook.chat.service.ChatPromptService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 프롬프트 조회는 관리자 화면에만 필요해서 여기에 둡니다.
// 고치는 일은 이전 본문을 이력에 남기는 규칙이 있어 core의 ChatPromptService에 맡깁니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatPromptAdminService {

    private final ChatPromptRepository chatPromptRepository;
    private final ChatPromptHistoryRepository chatPromptHistoryRepository;
    private final ChatPromptService chatPromptService;

    public List<AdminChatPromptResponse> findAll() {
        return chatPromptRepository.findAllByOrderByIdAsc().stream().map(AdminChatPromptResponse::from).toList();
    }

    public AdminChatPromptResponse get(Long promptId) {
        return AdminChatPromptResponse.from(chatPromptService.get(promptId));
    }

    public ChatPromptForm form(Long promptId) {
        return ChatPromptForm.from(chatPromptService.get(promptId));
    }

    public Page<ChatPromptHistoryResponse> histories(Long promptId, Pageable pageable) {
        return chatPromptHistoryRepository.findByChatPromptIdOrderByIdDesc(promptId, pageable)
                .map(ChatPromptHistoryResponse::from);
    }
}
