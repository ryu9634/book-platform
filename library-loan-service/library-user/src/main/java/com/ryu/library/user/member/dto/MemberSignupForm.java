package com.ryu.library.user.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원가입 화면 폼. Thymeleaf th:field 바인딩을 위해 getter/setter 클래스로 두고, 서비스에는 API와 같은 요청 DTO로 넘깁니다.
@Getter
@Setter
@NoArgsConstructor
public class MemberSignupForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자 4~20자입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자입니다.")
    private String password;

    @NotBlank(message = "비밀번호를 한 번 더 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 50, message = "이름은 50자 이하입니다.")
    private String name;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이하입니다.")
    private String email;

    @Pattern(regexp = "^$|^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호는 010-0000-0000 형식입니다.")
    private String phone;

    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirm);
    }

    public MemberSignupRequest toRequest() {
        return new MemberSignupRequest(loginId, password, name, email, phone == null || phone.isBlank() ? null : phone);
    }

    @Override
    public String toString() {
        return "MemberSignupForm[loginId=" + loginId + "]";
    }
}
