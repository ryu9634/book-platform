package com.ryu.ebook.user.chat.dto;

import com.ryu.ebook.chat.domain.ChatQuota;
import java.util.List;

// 챗봇 화면에 필요한 것 전부. 쓸 수 없는 상태면 왜 쓸 수 없는지도 함께 내려 화면에서 판단하지 않게 합니다.
public record ChatRoomResponse(boolean available, String unavailableReason, long dailyTokenLimit,
        long remainingTokens, int remainingPercent, List<ChatMessageResponse> messages) {

    public static ChatRoomResponse of(ChatQuota quota, List<ChatMessageResponse> messages) {
        return new ChatRoomResponse(quota.isAvailable(), reason(quota), quota.dailyTokenLimit(),
                quota.remainingTokens(), percent(quota), messages);
    }

    private static String reason(ChatQuota quota) {
        if (!quota.serviceEnabled() || quota.isGlobalLimitReached()) {
            return "AI 도서 추천을 잠시 중단했습니다. 나중에 다시 시도해 주세요.";
        }
        if (!quota.memberEnabled()) {
            return "이 계정은 AI 도서 추천을 쓸 수 없습니다.";
        }
        if (quota.isMemberLimitReached()) {
            return "오늘 쓸 수 있는 AI 추천 분량을 모두 썼습니다. 내일 다시 이용해 주세요.";
        }
        return null;
    }

    private static int percent(ChatQuota quota) {
        if (quota.dailyTokenLimit() <= 0) {
            return 0;
        }
        return (int) Math.round(quota.remainingTokens() * 100.0 / quota.dailyTokenLimit());
    }
}
