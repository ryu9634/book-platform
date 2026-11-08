package com.ryu.ebook.chat.repository;

// 관리자 사용량 화면의 한 줄. 회원 이름을 함께 보여 주려고 MEMBER를 조인해 가져옵니다.
public record ChatUsageView(Long memberId, String loginId, String memberName, long inputTokens, long outputTokens,
        long cacheCreateTokens, long cacheReadTokens, int requestCount) {

    public long totalTokens() {
        return inputTokens + outputTokens + cacheCreateTokens + cacheReadTokens;
    }
}
