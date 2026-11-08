package com.ryu.ebook.chat.domain;

// 한 번의 모델 호출이 쓴 토큰. 캐시 토큰은 입력·출력과 단가가 달라 따로 셉니다.
// Anthropic 응답에서 입력 토큰에는 캐시로 읽은 토큰이 포함되지 않으므로 네 값을 모두 더해야 총량이 됩니다.
public record TokenUsage(long inputTokens, long outputTokens, long cacheCreateTokens, long cacheReadTokens) {

    public static final TokenUsage NONE = new TokenUsage(0, 0, 0, 0);

    public static TokenUsage of(long inputTokens, long outputTokens) {
        return new TokenUsage(inputTokens, outputTokens, 0, 0);
    }

    public long totalTokens() {
        return inputTokens + outputTokens + cacheCreateTokens + cacheReadTokens;
    }
}
