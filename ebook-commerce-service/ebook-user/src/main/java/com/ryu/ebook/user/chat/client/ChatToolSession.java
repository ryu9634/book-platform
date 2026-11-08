package com.ryu.ebook.user.chat.client;

import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.model.ToolContext;

// 질문 한 건 동안만 사는 도구 호출 상태. ToolContext에 담아 넘깁니다.
// 회원 번호를 여기에 담는 것이 중요합니다. 도구 파라미터로 받으면 모델이 남의 회원 번호를 만들어 넣을 수 있습니다.
public final class ChatToolSession {

    public static final String KEY = "ebookChatToolSession";

    private final Long memberId;
    private final AtomicInteger remainingSearches;
    // 도구가 서로 다른 스레드에서 병렬로 불릴 수 있어 동시성에 안전한 목록을 씁니다.
    private final List<EbookSummaryResponse> shownBooks = new CopyOnWriteArrayList<>();

    public ChatToolSession(Long memberId, int maxSearches) {
        this.memberId = memberId;
        this.remainingSearches = new AtomicInteger(maxSearches);
    }

    public static ChatToolSession from(ToolContext toolContext) {
        Object value = toolContext == null ? null : toolContext.getContext().get(KEY);
        if (value instanceof ChatToolSession session) {
            return session;
        }
        throw new IllegalStateException("chat tool session is missing");
    }

    public Long memberId() {
        return memberId;
    }

    // 검색 예산을 하나 씁니다. 남아 있지 않으면 false를 돌려주고, 도구는 더 검색할 수 없다고 답합니다.
    public boolean consumeSearch() {
        return remainingSearches.getAndDecrement() > 0;
    }

    public void addShown(List<EbookSummaryResponse> books) {
        shownBooks.addAll(books);
    }

    public List<EbookSummaryResponse> shownBooks() {
        return List.copyOf(shownBooks);
    }
}
