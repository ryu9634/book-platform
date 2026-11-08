package com.ryu.ebook.user.chat.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// AI 도서 추천 챗봇 설정. 비용과 응답 시간에 바로 영향을 주는 값이라 코드에 숫자를 두지 않습니다.
// 운영 중 급히 바꿔야 하는 값(전체 중단, 기본 한도)은 여기가 아니라 CHAT_SETTING 테이블에 있습니다.
@ConfigurationProperties(prefix = "ebook.chat")
public record ChatProperties(
        // 모델 이름. 바꾸면 ebook.chat-pricing의 단가도 함께 바꿉니다.
        String model,
        // 추론 깊이. 채팅은 LOW로 충분하고, 올리면 답변이 느려지고 출력 토큰이 늘어납니다.
        String effort,
        // 한 답변의 출력 토큰 상한. 체감 속도와 비용을 결정하는 값입니다.
        int maxOutputTokens,
        // 다시 실어 보낼 지난 메시지 수. 늘리면 맥락이 좋아지고 입력 토큰이 그만큼 늘어납니다.
        int memoryWindow,
        // 한 질문에 허용하는 전자책 검색 횟수. 모델이 검색을 반복하며 토큰을 태우는 것을 막습니다.
        int maxToolCalls,
        // 검색 한 번에 모델에 보여 줄 전자책 수.
        int candidateSize,
        // 같은 회원의 질문 사이 최소 간격.
        Duration minInterval) {
}
