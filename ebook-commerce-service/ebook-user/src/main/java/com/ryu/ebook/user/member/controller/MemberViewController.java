package com.ryu.ebook.user.member.controller;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.member.dto.MemberSignupForm;
import com.ryu.ebook.user.member.dto.PasswordChangeForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ryu.ebook.user.member.service.MemberService;
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

    @GetMapping("/members/me")
    public String myPage(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("me", memberService.getMyPage(loginMember.getMemberId()));
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "member/me";
    }

    @PostMapping("/members/me/password")
    public String changePassword(@AuthenticationPrincipal LoginMember loginMember,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (!form.isConfirmed()) {
            bindingResult.rejectValue("newPasswordConfirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
        }
        if (!bindingResult.hasErrors()) {
            try {
                memberService.changePassword(loginMember.getMemberId(), form.getCurrentPassword(), form.getNewPassword());
                redirectAttributes.addFlashAttribute("message", "비밀번호를 바꿨습니다.");
                return "redirect:/members/me";
            } catch (BusinessException e) {
                bindingResult.rejectValue("currentPassword", e.getErrorCode().getCode(), e.getMessage());
            }
        }
        model.addAttribute("me", memberService.getMyPage(loginMember.getMemberId()));
        return "member/me";
    }
}
