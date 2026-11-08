package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.domain.ChatPrompt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatPromptForm {

    // 프롬프트는 질문마다 입력 토큰으로 나가므로 길이를 제한합니다. 엔티티도 같은 값으로 검사합니다.
    public static final int MAX_LENGTH = 10_000;

    @NotBlank(message = "프롬프트 본문을 입력해 주세요.")
    @Size(max = MAX_LENGTH, message = "프롬프트는 10000자 이하로 써 주세요.")
    private String content;

    public static ChatPromptForm from(ChatPrompt prompt) {
        ChatPromptForm form = new ChatPromptForm();
        form.content = prompt.getContent();
        return form;
    }
}
