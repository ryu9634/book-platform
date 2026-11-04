package com.ryu.library.user.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeForm {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자입니다.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호를 한 번 더 입력해 주세요.")
    private String newPasswordConfirm;

    public boolean isConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }

    @Override
    public String toString() {
        return "PasswordChangeForm[***]";
    }
}
