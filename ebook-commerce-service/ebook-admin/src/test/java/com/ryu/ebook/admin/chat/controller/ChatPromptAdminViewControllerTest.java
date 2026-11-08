package com.ryu.ebook.admin.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.admin.chat.dto.AdminChatPromptResponse;
import com.ryu.ebook.admin.chat.dto.ChatPromptForm;
import com.ryu.ebook.admin.chat.service.ChatPromptAdminService;
import com.ryu.ebook.admin.global.security.LoginAdmin;
import com.ryu.ebook.admin.support.AdminWebSecurityTest;
import com.ryu.ebook.chat.service.ChatPromptService;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatPromptAdminViewController.class)
@AdminWebSecurityTest
class ChatPromptAdminViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatPromptAdminService chatPromptAdminService;

    @MockitoBean
    ChatPromptService chatPromptService;

    private LoginAdmin loginAdmin;

    @BeforeEach
    void setUp() {
        loginAdmin = LoginAdmin.from(MemberFixture.activeMember("admin01", "admin01@example.com"));
        ReflectionTestUtils.setField(loginAdmin, "memberId", 9L);
        given(chatPromptAdminService.get(1L)).willReturn(new AdminChatPromptResponse(1L, "BOOK_RECOMMEND",
                "도서 추천 시스템 프롬프트", "기존 본문", true, 9L, LocalDateTime.of(2026, 11, 8, 10, 0)));
        given(chatPromptAdminService.form(1L)).willReturn(new ChatPromptForm());
        given(chatPromptAdminService.histories(eq(1L), any()))
                .willReturn(Page.empty(PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("수정 화면은 프롬프트와 변경 이력을 함께 보여 준다")
    void editForm_이력함께() throws Exception {
        mockMvc.perform(get("/admin/chat-prompts/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("prompt", "chatPromptForm", "historyPage"));
    }

    @Test
    @DisplayName("프롬프트를 저장하면 고친 관리자 번호와 함께 넘기고 다시 수정 화면으로 보낸다")
    void edit_성공_리다이렉트() throws Exception {
        mockMvc.perform(post("/admin/chat-prompts/1").with(user(loginAdmin)).with(csrf())
                        .param("content", "새 프롬프트 본문"))
                .andExpect(redirectedUrl("/admin/chat-prompts/1"))
                .andExpect(flash().attribute("message", "프롬프트를 저장했습니다."));
        verify(chatPromptService).changeContent(1L, "새 프롬프트 본문", 9L);
    }

    @Test
    @DisplayName("빈 프롬프트는 저장하지 않고 그 자리에서 오류를 보여 준다")
    void edit_빈값_필드오류() throws Exception {
        mockMvc.perform(post("/admin/chat-prompts/1").with(user(loginAdmin)).with(csrf()).param("content", " "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("chatPromptForm", "content"));
        verify(chatPromptService, never()).changeContent(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("저장 중 업무 예외가 나면 폼 위에 이유를 보여 주고, CSRF 토큰이 없는 요청은 받지 않는다")
    void edit_업무예외_폼에표시() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                .given(chatPromptService).changeContent(eq(1L), any(), eq(9L));

        // when, then
        mockMvc.perform(post("/admin/chat-prompts/1").with(user(loginAdmin)).with(csrf()).param("content", "본문"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("chatPromptForm"));
        mockMvc.perform(post("/admin/chat-prompts/1").with(user(loginAdmin)).param("content", "본문"))
                .andExpect(redirectedUrl("/admin/login?expired"));
    }

    @Test
    @DisplayName("사용 여부 토글은 목록으로 돌아가 결과를 안내한다")
    void changeEnabled_토글() throws Exception {
        mockMvc.perform(post("/admin/chat-prompts/1/enabled").with(user(loginAdmin)).with(csrf())
                        .param("enabled", "false"))
                .andExpect(redirectedUrl("/admin/chat-prompts"))
                .andExpect(flash().attribute("message", "프롬프트를 중단했습니다."));
        verify(chatPromptService).changeEnabled(1L, false, 9L);
    }
}
