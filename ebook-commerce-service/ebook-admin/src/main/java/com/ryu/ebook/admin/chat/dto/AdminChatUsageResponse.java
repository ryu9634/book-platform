package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.config.ChatPricing;
import com.ryu.ebook.chat.repository.ChatUsageView;

// 사용량 화면의 한 줄. 토큰 수만 보면 얼마가 나갔는지 알 수 없으므로 추정 비용을 함께 계산해 둡니다.
public record AdminChatUsageResponse(Long memberId, String loginId, String memberName, long inputTokens,
        long outputTokens, long cacheTokens, int requestCount, long totalTokens, double estimatedUsd) {

    public static AdminChatUsageResponse of(ChatUsageView view, ChatPricing pricing) {
        return new AdminChatUsageResponse(view.memberId(), view.loginId(), view.memberName(), view.inputTokens(),
                view.outputTokens(), view.cacheCreateTokens() + view.cacheReadTokens(), view.requestCount(),
                view.totalTokens(),
                pricing.estimateUsd(view.inputTokens(), view.outputTokens(), view.cacheCreateTokens(),
                        view.cacheReadTokens()));
    }
}
