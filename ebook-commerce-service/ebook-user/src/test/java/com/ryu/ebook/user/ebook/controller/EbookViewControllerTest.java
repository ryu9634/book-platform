package com.ryu.ebook.user.ebook.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.ebook.dto.EbookDetailResponse;
import com.ryu.ebook.user.ebook.dto.EbookSort;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import com.ryu.ebook.user.support.WebSecurityTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {EbookViewController.class, EbookApiController.class})
@WebSecurityTest
class EbookViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EbookQueryService ebookQueryService;

    private final EbookSummaryResponse summary =
            new EbookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", null, 36000, 32400, 10, "IT", true);

    @Test
    @DisplayName("비로그인도 목록을 보고, 정렬 값을 받아 서비스에 넘기며 할인율과 판매가가 보인다")
    void list_정렬_할인율() throws Exception {
        given(ebookQueryService.search(any(), any(), eq(EbookSort.PRICE_ASC), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/ebooks").param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10%")))
                .andExpect(content().string(containsString("32,400원")));
    }

    @Test
    @DisplayName("판매 중지 도서는 상세에서 안내하고, 숨긴 도서는 404로 답한다")
    void detail_판매중지_숨김() throws Exception {
        given(ebookQueryService.getEbook(1L)).willReturn(new EbookDetailResponse(1L, null, "이펙티브 자바", "조슈아 블로크",
                "인사이트", null, null, 36000, 32400, 10, false, 500, null, null));
        given(ebookQueryService.getEbook(2L)).willThrow(new BusinessException(ErrorCode.EBOOK_NOT_FOUND));

        mockMvc.perform(get("/ebooks/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("판매 중지된 도서입니다.")));
        mockMvc.perform(get("/ebooks/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ebooks/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E001"));
    }

    @Test
    @DisplayName("정해진 정렬 값이 아니면 400으로 거부한다 (임의 컬럼 정렬 방지)")
    void list_잘못된정렬_400() throws Exception {
        mockMvc.perform(get("/api/v1/ebooks").param("sort", "PASSWORD"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인한 회원에게 보유 중이면 서재에서 읽기, 장바구니에 있으면 장바구니 보기를 보여 준다")
    void detail_구매상태_버튼() throws Exception {
        given(ebookQueryService.getEbook(1L)).willReturn(new EbookDetailResponse(1L, null, "이펙티브 자바", "조슈아 블로크",
                "인사이트", null, null, 36000, 32400, 10, true, 500, null, null));
        var loginMember = com.ryu.ebook.user.global.security.LoginMember.from(com.ryu.ebook.member.MemberFixture.activeMember());
        org.springframework.test.util.ReflectionTestUtils.setField(loginMember, "memberId", 5L);
        var asMember = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(loginMember);
        given(ebookQueryService.purchaseState(5L, 1L))
                .willReturn(com.ryu.ebook.user.ebook.dto.PurchaseState.OWNED)
                .willReturn(com.ryu.ebook.user.ebook.dto.PurchaseState.IN_CART)
                .willReturn(com.ryu.ebook.user.ebook.dto.PurchaseState.CAN_PURCHASE);

        mockMvc.perform(get("/ebooks/1").with(asMember)).andExpect(content().string(containsString("서재에서 읽기")));
        mockMvc.perform(get("/ebooks/1").with(asMember)).andExpect(content().string(containsString("장바구니 보기")));
        mockMvc.perform(get("/ebooks/1").with(asMember))
                .andExpect(content().string(containsString("장바구니 담기")))
                .andExpect(content().string(containsString("바로 구매")));
    }
}
