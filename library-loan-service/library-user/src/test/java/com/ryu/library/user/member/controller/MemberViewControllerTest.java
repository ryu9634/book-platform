package com.ryu.library.user.member.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.user.member.dto.MemberResponse;
import com.ryu.library.user.member.service.MemberService;
import com.ryu.library.user.support.WebSecurityTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(MemberViewController.class)
@WebSecurityTest
class MemberViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    private MockHttpServletRequestBuilder signup(String passwordConfirm) {
        return post("/members/new").with(csrf())
                .param("loginId", "reader01").param("password", "plain-password").param("passwordConfirm", passwordConfirm)
                .param("name", "홍길동").param("email", "reader01@example.com");
    }

    @Test
    @DisplayName("회원가입 화면은 로그인 없이 열린다")
    void signupForm_비로그인_200() throws Exception {
        mockMvc.perform(get("/members/new")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("가입에 성공하면 로그인 화면으로 보내고 안내 메시지를 보여 준다")
    void signup_성공_로그인으로() throws Exception {
        // given
        given(memberService.signup(any())).willReturn(new MemberResponse(1L, "reader01", "홍길동", "reader01@example.com"));

        // when, then
        mockMvc.perform(signup("plain-password"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "가입했습니다. 로그인해 주세요."));
    }

    @Test
    @DisplayName("비밀번호 확인이 다르면 가입하지 않고 해당 칸에 오류를 보여 준다")
    void signup_비밀번호불일치_필드오류() throws Exception {
        mockMvc.perform(signup("different"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("signupForm", "passwordConfirm"));
        verify(memberService, never()).signup(any());
    }

    @Test
    @DisplayName("이미 사용 중인 아이디면 아이디 칸 아래에 이유를 보여 준다")
    void signup_중복아이디_아이디칸오류() throws Exception {
        // given
        given(memberService.signup(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        // when, then
        mockMvc.perform(signup("plain-password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("signupForm", "loginId"))
                .andExpect(content().string(containsString("이미 사용 중인 아이디입니다.")));
    }
}
