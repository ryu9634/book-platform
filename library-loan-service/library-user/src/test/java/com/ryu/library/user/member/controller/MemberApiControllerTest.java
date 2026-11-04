package com.ryu.library.user.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.user.member.dto.MemberResponse;
import com.ryu.library.user.member.dto.MemberSignupRequest;
import com.ryu.library.user.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberApiController.class)
class MemberApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    private static final String VALID_BODY = """
            {"loginId": "reader01", "password": "plain-password", "name": "홍길동", "email": "reader01@example.com"}
            """;

    @Test
    @DisplayName("가입에 성공하면 201과 회원 정보를 돌려준다")
    void signup_정상요청_201() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class)))
                .willReturn(new MemberResponse(1L, "reader01", "홍길동", "reader01@example.com"));

        // when, then
        mockMvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.loginId").value("reader01"));
    }

    @Test
    @DisplayName("입력값이 형식에 맞지 않으면 400과 필드 오류 목록을 돌려준다")
    void signup_형식오류_400() throws Exception {
        // given
        String body = """
                {"loginId": "A!", "password": "short", "name": "", "email": "not-email"}
                """;

        // when, then
        mockMvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001"))
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(4));
    }

    @Test
    @DisplayName("이미 사용 중인 아이디면 409와 회원 에러 코드를 돌려준다")
    void signup_중복아이디_409() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        // when, then
        mockMvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("M001"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 500이 아니라 400을 돌려준다")
    void signup_깨진JSON_400() throws Exception {
        // given
        String body = "{\"loginId\":";

        // when, then
        mockMvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    @DisplayName("JSON이 아닌 형식으로 보내면 415를 돌려준다")
    void signup_지원하지않는형식_415() throws Exception {
        // when, then
        mockMvc.perform(post("/api/v1/members").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }
}
