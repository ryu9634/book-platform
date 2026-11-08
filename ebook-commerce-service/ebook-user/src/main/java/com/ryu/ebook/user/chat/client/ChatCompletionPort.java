package com.ryu.ebook.user.chat.client;

import java.util.List;

// 모델 호출을 이 인터페이스 하나로 감쌉니다.
// 덕분에 서비스 테스트에서 실제 API를 부르지 않아도 되고, 나중에 모델 제공자를 바꿀 때 구현체만 교체합니다.
public interface ChatCompletionPort {

    ChatCompletion answer(String systemPrompt, List<ChatTurn> history, String question, Long memberId);
}
