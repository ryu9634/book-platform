package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.config.ChatPricing;
import com.ryu.ebook.chat.repository.ChatUsageTotal;

// 하루 합계. 전역 한도를 얼마나 썼는지도 함께 보여 줘야 브레이크를 걸 시점을 알 수 있습니다.
public record ChatUsageSummary(long totalTokens, long requestCount, double estimatedUsd, long globalDailyTokenLimit,
        int usedPercent) {

    public static ChatUsageSummary of(ChatUsageTotal total, ChatPricing pricing, long globalDailyTokenLimit) {
        double usd = pricing.estimateUsd(total.input(), total.output(), total.cacheCreate(), total.cacheRead());
        int percent = globalDailyTokenLimit <= 0 ? 0
                : (int) Math.min(100, Math.round(total.totalTokens() * 100.0 / globalDailyTokenLimit));
        return new ChatUsageSummary(total.totalTokens(), total.requests(), usd, globalDailyTokenLimit, percent);
    }
}
