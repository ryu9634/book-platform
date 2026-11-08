package com.ryu.ebook.chat.domain;

// 지금 이 회원이 챗봇을 쓸 수 있는지, 오늘 얼마나 남았는지. 화면에 남은 양을 보여 줄 때도 씁니다.
public record ChatQuota(boolean serviceEnabled, boolean memberEnabled, long dailyTokenLimit, long usedTokens,
        long globalDailyTokenLimit, long globalUsedTokens) {

    public boolean isAvailable() {
        return serviceEnabled && memberEnabled && !isMemberLimitReached() && !isGlobalLimitReached();
    }

    public boolean isMemberLimitReached() {
        return usedTokens >= dailyTokenLimit;
    }

    public boolean isGlobalLimitReached() {
        return globalUsedTokens >= globalDailyTokenLimit;
    }

    public long remainingTokens() {
        return Math.max(0, dailyTokenLimit - usedTokens);
    }
}
