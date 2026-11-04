package com.ryu.library.user.reservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.service.ReservationService;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.reservation.dto.ReservationResponse;
import com.ryu.library.user.reservation.service.ReservationQueryService;
import com.ryu.library.user.support.WebSecurityTest;
import java.time.LocalDateTime;
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

@WebMvcTest(controllers = {ReservationViewController.class, ReservationApiController.class})
@WebSecurityTest
class ReservationViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReservationService reservationService;

    @MockitoBean
    ReservationQueryService reservationQueryService;

    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        loginMember = LoginMember.from(MemberFixture.activeMember());
        ReflectionTestUtils.setField(loginMember, "memberId", 5L);
    }

    @Test
    @DisplayName("예약에 성공하면 내 예약으로, 예약할 수 없으면 도서 상세로 돌아가 이유를 보여 준다")
    void reserve_성공_실패() throws Exception {
        given(reservationService.reserve(5L, 1L)).willReturn(10L);
        given(reservationService.reserve(5L, 2L)).willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_ALLOWED_AVAILABLE));

        mockMvc.perform(post("/reservations").with(user(loginMember)).with(csrf()).param("bookId", "1"))
                .andExpect(redirectedUrl("/reservations/my"));
        mockMvc.perform(post("/reservations").with(user(loginMember)).with(csrf()).param("bookId", "2"))
                .andExpect(redirectedUrl("/books/2"))
                .andExpect(flash().attribute("errorMessage", "대출 가능한 권이 있어 예약할 수 없습니다. 바로 대출해 주세요."));
    }

    @Test
    @DisplayName("내 예약 화면은 대기 순번과 수령 가능 상태를 보여 준다")
    void myReservations_상태표시() throws Exception {
        given(reservationQueryService.myActiveReservations(5L)).willReturn(List.of(
                new ReservationResponse(10L, 1L, "이펙티브 자바", LocalDateTime.of(2026, 11, 1, 10, 0),
                        ReservationStatus.WAITING, null, 2L),
                new ReservationResponse(11L, 2L, "클린 코드", LocalDateTime.of(2026, 11, 1, 10, 0),
                        ReservationStatus.NOTIFIED, LocalDateTime.of(2026, 11, 7, 10, 0), null)));

        mockMvc.perform(get("/reservations/my").with(user(loginMember)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2번째 대기 중")))
                .andExpect(content().string(containsString("수령 가능")))
                .andExpect(content().string(containsString("대출하러 가기")));
    }

    @Test
    @DisplayName("이미 끝난 예약을 취소하면 내 예약 화면에 이유를 보여 준다")
    void cancel_끝난예약_안내() throws Exception {
        willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE)).given(reservationService).cancel(5L, 10L);

        mockMvc.perform(post("/reservations/10/cancel").with(user(loginMember)).with(csrf()))
                .andExpect(redirectedUrl("/reservations/my"))
                .andExpect(flash().attribute("errorMessage", "이미 끝난 예약입니다."));
    }

    @Test
    @DisplayName("API로 예약하고 취소한다")
    void api_예약_취소() throws Exception {
        var token = jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
        given(reservationService.reserve(5L, 1L)).willReturn(10L);

        mockMvc.perform(post("/api/v1/reservations").with(token).contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reservationId").value(10));
        mockMvc.perform(delete("/api/v1/reservations/10").with(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
