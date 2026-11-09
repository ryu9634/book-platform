package com.ryu.ebook.user.chat.client;

import java.util.List;
import java.util.function.Consumer;

// 모델 호출을 이 인터페이스 하나로 감쌉니다.
// 덕분에 서비스 테스트에서 실제 API를 부르지 않아도 되고, 나중에 모델 제공자를 바꿀 때 구현체만 교체합니다.
public interface ChatCompletionPort {

    ChatCompletion answer(String systemPrompt, List<ChatTurn> history, String question, Long memberId);

    // 답변을 조각으로 받습니다. onChunk는 조각이 올 때마다 불리고, 돌려주는 값은 완성된 답변입니다.
    // 반응형 타입(Flux)을 이 인터페이스에 노출하지 않는 이유: 서비스와 컨트롤러가 모델 쪽 사정을 알지 않아야 하고,
    // 테스트에서 대역을 만들 때도 조각을 몇 번 넘겨 주는 것으로 끝나기 때문입니다.
    ChatCompletion answerStreaming(String systemPrompt, List<ChatTurn> history, String question, Long memberId,
            Consumer<String> onChunk);
}
