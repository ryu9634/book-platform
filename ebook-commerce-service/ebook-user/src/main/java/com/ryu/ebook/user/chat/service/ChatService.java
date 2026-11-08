package com.ryu.ebook.user.chat.service;

import com.ryu.ebook.chat.domain.ChatPrompt;
import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.service.ChatPromptService;
import com.ryu.ebook.chat.service.ChatQuotaService;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.chat.client.ChatCompletion;
import com.ryu.ebook.user.chat.client.ChatCompletionPort;
import com.ryu.ebook.user.chat.client.ChatTurn;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.chat.dto.ChatAnswerResponse;
import com.ryu.ebook.user.chat.dto.ChatQuestionForm;
import com.ryu.ebook.user.chat.dto.ChatRoomResponse;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 질문 한 건의 흐름을 조립합니다. 이 클래스에는 @Transactional을 붙이지 않습니다.
// 모델 호출은 외부 호출이라 트랜잭션 밖에서 해야 하고, 저장은 ChatSessionService의 트랜잭션에 맡깁니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 답변이 출력 상한에 걸려 끊겼을 때 덧붙이는 안내. 끊긴 문장을 그대로 두면 회원이 오해합니다.
    private static final String TRUNCATED_NOTICE = "\n\n(답변이 길어져 여기서 끊었습니다. 더 좁혀서 다시 물어봐 주세요.)";

    // 답변에 제목이 나온 책만 화면에 카드로 띄웁니다. 너무 많이 붙지 않게 상한을 둡니다.
    private static final int MAX_RECOMMENDED_CARDS = 3;

    private final ChatQuotaService chatQuotaService;
    private final ChatPromptService chatPromptService;
    private final ChatSessionService chatSessionService;
    private final ChatCompletionPort chatCompletionPort;
    private final ChatProperties chatProperties;
    private final Clock clock;

    public ChatRoomResponse room(Long memberId) {
        ChatQuota quota = chatQuotaService.quota(memberId);
        return ChatRoomResponse.of(quota, chatSessionService.findActive(memberId)
                .map(session -> chatSessionService.messages(session.getId()))
                .orElse(List.of()));
    }

    public ChatAnswerResponse ask(Long memberId, String question) {
        String normalized = normalize(question);
        chatQuotaService.ensureAvailable(memberId);

        ChatSession session = chatSessionService.openIfAbsent(memberId);
        ensureNotTooFast(session);
        String systemPrompt = chatPromptService.activeContent(ChatPrompt.BOOK_RECOMMEND);
        List<ChatTurn> history = chatSessionService.recentTurns(session.getId());

        ChatCompletion completion = chatCompletionPort.answer(systemPrompt, history, normalized, memberId);

        // 거절이든 중단이든 토큰은 이미 쓴 상태입니다. 그래서 답변을 쓸 수 있는지 판단하기 전에 먼저 기록합니다.
        chatQuotaService.record(memberId, completion.usage());
        if (completion.rejected()) {
            log.warn("chat answer rejected by model. memberId={}", memberId);
            throw new BusinessException(ErrorCode.CHAT_ANSWER_REJECTED);
        }

        String answer = completion.truncated() ? completion.answer() + TRUNCATED_NOTICE : completion.answer();
        List<EbookSummaryResponse> recommended = mentionedBooks(answer, completion.shownBooks());
        chatSessionService.appendExchange(session.getId(), normalized, answer, completion.usage(),
                recommended.stream().map(EbookSummaryResponse::id).toList());
        log.info("chat answered. memberId={}, sessionId={}, totalTokens={}", memberId, session.getId(),
                completion.usage().totalTokens());
        return new ChatAnswerResponse(answer, recommended);
    }

    public void startNewConversation(Long memberId) {
        chatSessionService.closeActive(memberId);
    }

    private String normalize(String question) {
        String trimmed = question == null ? "" : question.trim();
        if (trimmed.isEmpty() || trimmed.length() > ChatQuestionForm.MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return trimmed;
    }

    // 연타로 비용이 새는 것을 막습니다. 마지막 대화 시각이 DB에 있어 여러 대로 띄워도 같이 동작합니다.
    private void ensureNotTooFast(ChatSession session) {
        LocalDateTime lastMessageAt = session.getLastMessageAt();
        if (lastMessageAt == null) {
            return;
        }
        if (lastMessageAt.plus(chatProperties.minInterval()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CHAT_TOO_MANY_REQUESTS);
        }
    }

    // 프롬프트가 검색 결과의 제목을 그대로 쓰라고 지시하므로, 제목이 들어 있는지로 어떤 책을 추천했는지 알 수 있습니다.
    private List<EbookSummaryResponse> mentionedBooks(String answer, List<EbookSummaryResponse> shownBooks) {
        Map<Long, EbookSummaryResponse> matched = new LinkedHashMap<>();
        for (EbookSummaryResponse book : shownBooks) {
            if (matched.size() >= MAX_RECOMMENDED_CARDS) {
                break;
            }
            if (book.title() != null && !book.title().isBlank() && answer.contains(book.title())) {
                matched.putIfAbsent(book.id(), book);
            }
        }
        return List.copyOf(matched.values());
    }
}
