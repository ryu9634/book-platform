package com.ryu.ebook.user.chat.dto;

import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.util.List;

// 질문 한 건의 답변. 화면은 저장된 대화를 다시 그리고, API는 이 응답을 그대로 돌려줍니다.
public record ChatAnswerResponse(String answer, List<EbookSummaryResponse> recommendedBooks) {
}
