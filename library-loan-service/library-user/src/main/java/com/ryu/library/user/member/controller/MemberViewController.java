package com.ryu.library.user.member.controller;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.user.member.dto.MemberSignupForm;
import com.ryu.library.user.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MemberViewController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new MemberSignupForm());
        return "member/signup";
    }

    // 중복 아이디와 이메일은 해당 입력 칸 아래에 바로 보여 줍니다.
    @PostMapping("/members/new")
    public String signup(@Valid @ModelAttribute("signupForm") MemberSignupForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (!form.isPasswordConfirmed()) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "member/signup";
        }
        try {
            memberService.signup(form.toRequest());
        } catch (BusinessException e) {
            String field = e.getErrorCode() == ErrorCode.DUPLICATE_EMAIL ? "email" : "loginId";
            bindingResult.rejectValue(field, e.getErrorCode().getCode(), e.getMessage());
            return "member/signup";
        }
        redirectAttributes.addFlashAttribute("message", "가입했습니다. 로그인해 주세요.");
        return "redirect:/login";
    }
}
