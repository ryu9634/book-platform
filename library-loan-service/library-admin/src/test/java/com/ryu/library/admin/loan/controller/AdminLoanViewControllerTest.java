package com.ryu.library.admin.loan.controller;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.ryu.library.admin.loan.service.AdminLoanService;
import com.ryu.library.admin.support.AdminWebSecurityTest;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminLoanViewController.class)
@AdminWebSecurityTest
class AdminLoanViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminLoanService adminLoanService;

    @Test
    @DisplayName("데스크 반납 후 돌아갈 화면은 관리자 대출 화면만 허용한다 (열린 리다이렉트 방지)")
    void returnByDesk_돌아갈주소제한() throws Exception {
        mockMvc.perform(post("/admin/loans/1/return").with(user("librarian").roles("ADMIN")).with(csrf())
                        .param("back", "/admin/loans/overdue"))
                .andExpect(redirectedUrl("/admin/loans/overdue"))
                .andExpect(flash().attribute("message", "반납 처리했습니다."));
        mockMvc.perform(post("/admin/loans/1/return").with(user("librarian").roles("ADMIN")).with(csrf())
                        .param("back", "https://evil.example.com"))
                .andExpect(redirectedUrl("/admin/loans"));
        verify(adminLoanService, org.mockito.Mockito.times(2)).returnByDesk(1L);
    }

    @Test
    @DisplayName("이미 반납된 대출이면 이유를 보여 준다")
    void returnByDesk_이미반납_안내() throws Exception {
        willThrow(new BusinessException(ErrorCode.LOAN_ALREADY_RETURNED)).given(adminLoanService).returnByDesk(2L);

        mockMvc.perform(post("/admin/loans/2/return").with(user("librarian").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/loans"))
                .andExpect(flash().attribute("errorMessage", "이미 반납한 도서입니다."));
    }
}
