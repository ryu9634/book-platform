package com.ryu.library.user.loan.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.service.LoanService;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.loan.dto.LoanResponse;
import com.ryu.library.user.loan.service.LoanQueryService;
import com.ryu.library.user.support.WebSecurityTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {LoanViewController.class, LoanApiController.class})
@WebSecurityTest
class LoanViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoanService loanService;

    @MockitoBean
    LoanQueryService loanQueryService;

    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        loginMember = LoginMember.from(MemberFixture.activeMember());
        ReflectionTestUtils.setField(loginMember, "memberId", 5L);
    }

    @Test
    @DisplayName("대출에 성공하면 내 대출 화면으로 보낸다")
    void loan_성공_내대출로() throws Exception {
        // given
        given(loanService.loanBook(5L, 1L)).willReturn(100L);

        // when, then
        mockMvc.perform(post("/loans").with(user(loginMember)).with(csrf()).param("bookId", "1"))
                .andExpect(redirectedUrl("/loans/my"))
                .andExpect(flash().attribute("message", "대출했습니다. 반납 예정일을 확인해 주세요."));
    }

    @Test
    @DisplayName("대출 조건을 못 맞추면 도서 상세로 돌아가 이유를 보여 준다")
    void loan_조건불충족_상세로() throws Exception {
        // given
        given(loanService.loanBook(5L, 1L)).willThrow(new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED, 5));

        // when, then
        mockMvc.perform(post("/loans").with(user(loginMember)).with(csrf()).param("bookId", "1"))
                .andExpect(redirectedUrl("/books/1"))
                .andExpect(flash().attribute("errorMessage", "최대 대출 권수(5권)를 넘을 수 없습니다."));
    }

    @Test
    @DisplayName("다른 회원이 같은 권을 먼저 대출해 충돌하면 다시 시도하라고 안내한다")
    void loan_동시대출충돌_안내() throws Exception {
        // given
        given(loanService.loanBook(5L, 1L)).willThrow(new ObjectOptimisticLockingFailureException("BookCopy", 10L));

        // when, then
        mockMvc.perform(post("/loans").with(user(loginMember)).with(csrf()).param("bookId", "1"))
                .andExpect(redirectedUrl("/books/1"))
                .andExpect(flash().attribute("errorMessage", ErrorCode.CONCURRENT_MODIFICATION.getMessage()));
    }

    @Test
    @DisplayName("로그인하지 않으면 대출할 수 없고 로그인 화면으로 보낸다")
    void loan_비로그인_로그인으로() throws Exception {
        mockMvc.perform(post("/loans").with(csrf()).param("bookId", "1"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("내 대출 화면은 반납 예정일과 연체 일수를 보여 준다")
    void myLoans_연체표시() throws Exception {
        // given
        given(loanQueryService.myActiveLoans(5L)).willReturn(List.of(new LoanResponse(100L, 1L, "이펙티브 자바",
                "LIB-000001-001", LocalDateTime.of(2026, 10, 1, 10, 0), LocalDate.of(2026, 10, 15), null,
                LoanStatus.OVERDUE, 0, true, 20)));

        // when, then
        mockMvc.perform(get("/loans/my").with(user(loginMember)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이펙티브 자바")))
                .andExpect(content().string(containsString("연체 20일")));
    }

    @Test
    @DisplayName("API 대출은 bookId와 bookCopyId 중 하나만 받는다")
    void api_loan_요청검증() throws Exception {
        var token = jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
        given(loanService.loanBook(5L, 1L)).willReturn(100L);

        mockMvc.perform(post("/api/v1/loans").with(token).contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.loanId").value(100));
        mockMvc.perform(post("/api/v1/loans").with(token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"bookCopyId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    @DisplayName("API에서 대출 조건을 못 맞추면 해당 에러 코드로 응답한다")
    void api_loan_조건불충족_에러코드() throws Exception {
        // given
        given(loanService.loanBook(5L, 1L)).willThrow(new BusinessException(ErrorCode.HAS_OVERDUE_LOAN));

        // when, then
        mockMvc.perform(post("/api/v1/loans")
                        .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("L002"));
    }
}
