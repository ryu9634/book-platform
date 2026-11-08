package com.ryu.ebook.user.chat.service;

import com.ryu.ebook.chat.domain.ChatMessage;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.ChatSessionStatus;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.chat.repository.ChatMessageRepository;
import com.ryu.ebook.chat.repository.ChatSessionRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.chat.client.ChatTurn;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.chat.dto.ChatMessageResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 대화 세션과 메시지를 다룹니다. 모델 호출은 여기서 하지 않습니다(외부 호출은 트랜잭션 밖에서 해야 하므로).
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatProperties chatProperties;
    private final Clock clock;

    public Optional<ChatSession> findActive(Long memberId) {
        return chatSessionRepository.findFirstByMemberIdAndStatusOrderByIdDesc(memberId, ChatSessionStatus.ACTIVE);
    }

    @Transactional
    public ChatSession openIfAbsent(Long memberId) {
        return findActive(memberId).orElseGet(() -> chatSessionRepository.save(ChatSession.open(memberId)));
    }

    // 새 대화: 지금 대화를 닫기만 합니다. 다음 질문이 들어올 때 새 세션이 열립니다.
    @Transactional
    public void closeActive(Long memberId) {
        findActive(memberId).ifPresent(session -> {
            session.close();
            log.info("chat session closed. sessionId={}, memberId={}", session.getId(), memberId);
        });
    }

    public List<ChatMessageResponse> messages(Long chatSessionId) {
        return chatMessageRepository.findByChatSessionIdOrderBySeqNoAsc(chatSessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    // 모델에 다시 실어 보낼 지난 대화. 최근 것부터 가져와 오래된 순서로 되돌립니다.
    public List<ChatTurn> recentTurns(Long chatSessionId) {
        List<ChatMessage> recent = chatMessageRepository.findByChatSessionIdOrderBySeqNoDesc(chatSessionId,
                PageRequest.of(0, chatProperties.memoryWindow()));
        return recent.reversed().stream()
                .map(message -> new ChatTurn(message.isQuestion(), message.getContent()))
                .toList();
    }

    @Transactional
    public void appendExchange(Long chatSessionId, String question, String answer, TokenUsage usage) {
        ChatSession session = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        int lastSeqNo = chatMessageRepository.findLastSeqNo(chatSessionId);
        chatMessageRepository.save(ChatMessage.question(chatSessionId, lastSeqNo + 1, question));
        chatMessageRepository.save(ChatMessage.answer(chatSessionId, lastSeqNo + 2, answer, usage));
        session.recordExchange(question, LocalDateTime.now(clock));
    }
}
