package com.ryu.library.user.book.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.user.book.dto.BookDetailResponse;
import com.ryu.library.user.book.dto.BookSummaryResponse;
import com.ryu.library.user.book.service.BookQueryService;
import com.ryu.library.user.support.WebSecurityTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {BookViewController.class, BookApiController.class})
@WebSecurityTest
class BookViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BookQueryService bookQueryService;

    private final BookSummaryResponse summary =
            new BookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", "인사이트", null, "컴퓨터/IT", 2, 3);

    @Test
    @DisplayName("비로그인도 도서 목록을 보고, 대출 가능 권 수가 뱃지로 보인다")
    void list_비로그인_목록() throws Exception {
        // given
        given(bookQueryService.search(eq("자바"), any(), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        // when, then
        mockMvc.perform(get("/books").param("keyword", "자바"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이펙티브 자바")))
                .andExpect(content().string(containsString("대출 가능 2/3")));
    }

    @Test
    @DisplayName("도서 상세에서 소장 자료별 상태를 보고, 비로그인이면 로그인 안내 버튼이 보인다")
    void detail_비로그인_로그인안내() throws Exception {
        // given
        given(bookQueryService.getBook(1L)).willReturn(new BookDetailResponse(1L, "9788966262281", "이펙티브 자바",
                "조슈아 블로크", "인사이트", null, null, null, "컴퓨터/IT",
                List.of(new BookDetailResponse.CopyItem(10L, "LIB-000001-001", BookCopyStatus.AVAILABLE),
                        new BookDetailResponse.CopyItem(11L, "LIB-000001-002", BookCopyStatus.ON_LOAN))));

        // when, then
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LIB-000001-002")))
                .andExpect(content().string(containsString("대출 가능 1 / 2")))
                .andExpect(content().string(containsString("로그인 후 대출 가능")));
    }

    @Test
    @DisplayName("없는 도서 상세는 404 오류 화면을 보여 준다")
    void detail_없는도서_404() throws Exception {
        // given
        given(bookQueryService.getBook(999L)).willThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // when, then
        mockMvc.perform(get("/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("도서를 찾을 수 없습니다.")));
    }

    @Test
    @DisplayName("도서 API는 토큰 없이 조회할 수 있고 페이지 형식으로 돌려준다")
    void api_비로그인_페이지응답() throws Exception {
        // given
        given(bookQueryService.search(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        // when, then
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("이펙티브 자바"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("없는 도서를 API로 조회하면 404 JSON을 돌려준다")
    void api_없는도서_404JSON() throws Exception {
        // given
        given(bookQueryService.getBook(999L)).willThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // when, then
        mockMvc.perform(get("/api/v1/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("B001"));
    }
}
