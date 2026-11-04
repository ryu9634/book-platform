package com.ryu.library.user.global.config;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.global.config.PasswordEncoderConfig;
import com.ryu.library.member.domain.Member;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.global.security.LoginMemberDetailsService;
import com.ryu.library.user.home.controller.HomeViewController;
import com.ryu.library.user.member.controller.LoginViewController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {HomeViewController.class, LoginViewController.class})
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    LoginMemberDetailsService loginMemberDetailsService;

    private LoginMember loginMember(String rawPassword) {
        Member member = Member.create("reader01", passwordEncoder.encode(rawPassword), "홍길동", "reader01@example.com", null);
        return LoginMember.from(member);
    }

    @Test
    @DisplayName("홈과 로그인 화면은 로그인하지 않아도 볼 수 있다")
    void 공개화면_비로그인_200() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인이 필요한 화면에 비로그인으로 접근하면 로그인 화면으로 보낸다")
    void 보호화면_비로그인_로그인화면으로() throws Exception {
        mockMvc.perform(get("/loans/my"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("아이디와 비밀번호가 맞으면 로그인하고 홈으로 보낸다")
    void 로그인_올바른비밀번호_성공() throws Exception {
        // given
        given(loginMemberDetailsService.loadUserByUsername("reader01")).willReturn(loginMember("plain-password"));

        // when, then
        mockMvc.perform(formLogin("/login").user("loginId", "reader01").password("plain-password"))
                .andExpect(authenticated().withUsername("reader01"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인 화면으로 돌려보내고 오류를 표시한다")
    void 로그인_틀린비밀번호_실패() throws Exception {
        // given
        given(loginMemberDetailsService.loadUserByUsername("reader01")).willReturn(loginMember("plain-password"));

        // when, then
        mockMvc.perform(formLogin("/login").user("loginId", "reader01").password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("로그인한 회원은 홈 화면 헤더에서 이름을 본다")
    void 홈_로그인회원_이름표시() throws Exception {
        mockMvc.perform(get("/").with(user(loginMember("plain-password"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("홍길동님")));
    }

    @Test
    @DisplayName("로그아웃은 CSRF 토큰이 있어야 하고, 성공하면 로그인 화면으로 보낸다")
    void 로그아웃_CSRF토큰_성공() throws Exception {
        mockMvc.perform(post("/logout").with(user(loginMember("plain-password"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(user(loginMember("plain-password"))).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("인증이 필요한 API에 비로그인으로 접근하면 로그인 화면이 아니라 401 JSON을 돌려준다")
    void API_비로그인_401JSON() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C006"));
    }
}
