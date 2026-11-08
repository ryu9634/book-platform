package com.ryu.ebook.chat.repository;

// 하루 전체 합계. 집계 함수 결과를 그대로 담으므로 래퍼 타입으로 받고, 비어 있으면 0으로 바꿔 씁니다.
public record ChatUsageTotal(Long inputTokens, Long outputTokens, Long cacheCreateTokens, Long cacheReadTokens,
        Long requestCount) {

    public long input() {
        return inputTokens == null ? 0 : inputTokens;
    }

    public long output() {
        return outputTokens == null ? 0 : outputTokens;
    }

    public long cacheCreate() {
        return cacheCreateTokens == null ? 0 : cacheCreateTokens;
    }

    public long cacheRead() {
        return cacheReadTokens == null ? 0 : cacheReadTokens;
    }

    public long requests() {
        return requestCount == null ? 0 : requestCount;
    }

    public long totalTokens() {
        return input() + output() + cacheCreate() + cacheRead();
    }
}
