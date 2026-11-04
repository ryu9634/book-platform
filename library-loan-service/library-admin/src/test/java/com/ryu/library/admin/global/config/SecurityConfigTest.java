package com.ryu.library.admin.global.config;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.admin.global.security.LoginAdmin;
import com.ryu.library.admin.global.security.LoginAdminDetailsService;
import com.ryu.library.admin.home.controller.AdminHomeViewController;
import com.ryu.library.global.config.PasswordEncoderConfig;
import com.ryu.library.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminHomeViewController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    LoginAdminDetailsService loginAdminDetailsService;

    @MockitoBean
    com.ryu.library.admin.home.service.DashboardService dashboardService;

    @org.junit.jupiter.api.BeforeEach
    void stubDashboard() {
        given(dashboardService.summary()).willReturn(new com.ryu.library.admin.home.service.DashboardService.Summary(1, 2, 3, 4));
    }

    private LoginAdmin loginAdmin() {
        return LoginAdmin.from(Member.createAdmin("librarian", passwordEncoder.encode("plain-password"), "사서",
                "librarian@example.com"));
    }

    @Test
    @DisplayName("로그인하지 않으면 관리자 화면 대신 관리자 로그인 화면으로 보낸다")
    void 관리자화면_비로그인_로그인화면으로() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
        mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자는 로그인하면 대시보드로 이동한다")
    void 로그인_관리자_성공() throws Exception {
        // given
        given(loginAdminDetailsService.loadUserByUsername("librarian")).willReturn(loginAdmin());

        // when, then
        mockMvc.perform(formLogin("/admin/login").user("loginId", "librarian").password("plain-password"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("일반 회원 아이디로는 관리자 로그인이 되지 않는다")
    void 로그인_일반회원_실패() throws Exception {
        // given
        given(loginAdminDetailsService.loadUserByUsername("reader01"))
                .willThrow(new UsernameNotFoundException("admin not found"));

        // when, then
        mockMvc.perform(formLogin("/admin/login").user("loginId", "reader01").password("plain-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    @DisplayName("ADMIN 권한이 없으면 관리자 화면에 들어갈 수 없다")
    void 관리자화면_USER권한_403() throws Exception {
        mockMvc.perform(get("/admin").with(user("reader01").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 화면 헤더에 로그인한 관리자 이름을 보여 준다")
    void 관리자화면_로그인_200() throws Exception {
        mockMvc.perform(get("/admin").with(user(loginAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 API는 비로그인이면 401 JSON을 돌려준다")
    void 관리자API_비로그인_401JSON() throws Exception {
        mockMvc.perform(get("/api/v1/admin/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("C006"));
    }
}
