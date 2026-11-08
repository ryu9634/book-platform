package com.ryu.ebook.user.chat.client;

import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.util.List;

// 모델이 돌려준 답변 한 건.
// rejected는 모델이 답변을 거절한 경우(stop_reason=refusal), truncated는 출력 토큰 상한에 걸려 문장이 끊긴 경우입니다.
// 둘 다 토큰은 이미 쓴 상태라 사용량은 그대로 기록합니다.
// shownBooks는 이번 답변을 만들면서 모델에 보여 준 전자책입니다. 답변에 제목이 나온 책을 화면에서 링크로 바꾸는 데 씁니다.
public record ChatCompletion(String answer, TokenUsage usage, boolean rejected, boolean truncated,
        List<EbookSummaryResponse> shownBooks) {
}
