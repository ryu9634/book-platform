package com.ryu.ebook.user.order.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.order.service.OrderService;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.service.PaymentService;
import com.ryu.ebook.user.global.security.LoginMember;
import com.ryu.ebook.user.order.dto.CheckoutResponse;
import com.ryu.ebook.user.order.service.OrderQueryService;
import com.ryu.ebook.user.support.WebSecurityTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {OrderViewController.class, OrderApiController.class})
@WebSecurityTest
class OrderViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    OrderQueryService orderQueryService;

    @MockitoBean
    PaymentService paymentService;

    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        loginMember = LoginMember.from(MemberFixture.activeMember());
        ReflectionTestUtils.setField(loginMember, "memberId", 5L);
    }

    @Test
    @DisplayName("주문서는 고른 전자책과 합계를 보여 주고, 주문할 수 없으면 장바구니로 돌아가 이유를 보여 준다")
    void checkout() throws Exception {
        given(orderQueryService.checkout(5L, List.of(1L)))
                .willReturn(new CheckoutResponse(List.of(new CheckoutResponse.Line(1L, "이펙티브 자바", 36000, 32400)), 32400));
        given(orderQueryService.checkout(5L, List.of(2L))).willThrow(new BusinessException(ErrorCode.ALREADY_OWNED));

        mockMvc.perform(get("/orders/checkout").param("ebookIds", "1").with(user(loginMember)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("결제 금액 32,400원")));
        mockMvc.perform(get("/orders/checkout").param("ebookIds", "2").with(user(loginMember)))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("errorMessage", "이미 내 서재에 있는 전자책입니다."));
    }

    @Test
    @DisplayName("주문을 만들면 결제 화면으로 가고, 승인되면 완료 화면, 실패하면 결제 화면으로 돌아가 사유를 보여 준다")
    void 주문_결제() throws Exception {
        given(orderService.create(5L, List.of(1L), PaymentMethod.MOCK)).willReturn("20261104-000001");
        given(paymentService.approve(5L, "20261104-000001", false))
                .willReturn(new PaymentService.PaymentOutcome(true, null));
        given(paymentService.approve(5L, "20261104-000001", true))
                .willReturn(new PaymentService.PaymentOutcome(false, "카드사 승인이 거절되었습니다."));

        mockMvc.perform(post("/orders").with(user(loginMember)).with(csrf()).param("ebookIds", "1"))
                .andExpect(redirectedUrl("/orders/20261104-000001/payment"));
        mockMvc.perform(post("/orders/20261104-000001/payment/approve").with(user(loginMember)).with(csrf()))
                .andExpect(redirectedUrl("/orders/20261104-000001/complete"));
        mockMvc.perform(post("/orders/20261104-000001/payment/approve").with(user(loginMember)).with(csrf())
                        .param("simulateFailure", "true"))
                .andExpect(redirectedUrl("/orders/20261104-000001/payment"))
                .andExpect(flash().attribute("errorMessage", "결제에 실패했습니다. 카드사 승인이 거절되었습니다."));
    }

    @Test
    @DisplayName("API 결제가 실패하면 402와 실패 사유를 담은 에러 코드로 응답한다")
    void api_결제실패_402() throws Exception {
        given(paymentService.approve(5L, "20261104-000001", true))
                .willReturn(new PaymentService.PaymentOutcome(false, "카드사 승인이 거절되었습니다."));

        mockMvc.perform(post("/api/v1/orders/20261104-000001/payment/approve")
                        .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"simulateFailure\":true}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error.code").value("P002"))
                .andExpect(jsonPath("$.error.message").value("결제에 실패했습니다. (카드사 승인이 거절되었습니다.)"));
    }

    @Test
    @DisplayName("API 주문은 전자책 목록이 비어 있으면 400으로 거부한다")
    void api_빈주문_400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ebookIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }
}
