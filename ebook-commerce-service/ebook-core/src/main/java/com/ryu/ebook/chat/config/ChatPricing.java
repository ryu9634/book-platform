package com.ryu.ebook.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// AI 챗봇 토큰 단가(100만 토큰당 달러). 관리자 사용량 화면에서 추정 비용을 보여 줄 때만 씁니다.
// 실제 청구는 Anthropic 콘솔 기준이므로 여기 값은 어림값입니다.
@ConfigurationProperties(prefix = "ebook.chat-pricing")
public record ChatPricing(double inputPerMillion, double outputPerMillion, double cacheCreatePerMillion,
        double cacheReadPerMillion) {

    public double estimateUsd(long inputTokens, long outputTokens, long cacheCreateTokens, long cacheReadTokens) {
        double total = inputTokens * inputPerMillion
                + outputTokens * outputPerMillion
                + cacheCreateTokens * cacheCreatePerMillion
                + cacheReadTokens * cacheReadPerMillion;
        return total / 1_000_000d;
    }
}
