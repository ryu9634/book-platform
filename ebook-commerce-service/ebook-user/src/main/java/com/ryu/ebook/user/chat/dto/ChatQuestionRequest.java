package com.ryu.ebook.user.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatQuestionRequest(
        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = ChatQuestionForm.MAX_LENGTH, message = "질문은 500자 이하로 써 주세요.")
        String question) {
}
