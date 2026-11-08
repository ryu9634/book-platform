package com.ryu.ebook.admin.chat.dto;

import com.ryu.ebook.chat.domain.ChatSetting;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatSettingForm {

    // 체크박스는 끄면 값이 오지 않으므로 기본값 false로 둡니다.
    private boolean enabled;

    @NotNull(message = "기본 일일 한도를 입력해 주세요.")
    @PositiveOrZero(message = "한도는 0 이상이어야 합니다.")
    private Long defaultDailyTokenLimit;

    @NotNull(message = "서비스 전체 일일 한도를 입력해 주세요.")
    @PositiveOrZero(message = "한도는 0 이상이어야 합니다.")
    private Long globalDailyTokenLimit;

    public static ChatSettingForm from(ChatSetting setting) {
        ChatSettingForm form = new ChatSettingForm();
        form.enabled = setting.isEnabled();
        form.defaultDailyTokenLimit = setting.getDefaultDailyTokenLimit();
        form.globalDailyTokenLimit = setting.getGlobalDailyTokenLimit();
        return form;
    }
}
