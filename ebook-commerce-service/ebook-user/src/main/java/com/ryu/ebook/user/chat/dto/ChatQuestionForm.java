package com.ryu.ebook.user.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatQuestionForm {

    // 질문이 길어지면 그만큼 입력 토큰이 늘어나므로 화면에서 먼저 막습니다.
    public static final int MAX_LENGTH = 500;

    @NotBlank(message = "질문을 입력해 주세요.")
    @Size(max = MAX_LENGTH, message = "질문은 500자 이하로 써 주세요.")
    private String question;
}
