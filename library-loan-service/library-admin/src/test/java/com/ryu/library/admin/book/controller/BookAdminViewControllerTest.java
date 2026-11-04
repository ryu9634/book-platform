package com.ryu.library.admin.book.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ryu.library.admin.book.dto.BookForm;
import com.ryu.library.admin.book.service.BookAdminService;
import com.ryu.library.admin.support.AdminWebSecurityTest;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookAdminViewController.class)
@AdminWebSecurityTest
class BookAdminViewControllerTest {

    private static final String ISBN = "9788966262281";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BookAdminService bookAdminService;

    private UserRequestPostProcessor admin() {
        return user("librarian").roles("ADMIN");
    }

    @Test
    @DisplayName("도서 목록 화면을 보여 준다")
    void list_관리자_200() throws Exception {
        // given
        given(bookAdminService.findBooks(any(), any())).willReturn(Page.empty());

        // when, then
        mockMvc.perform(get("/admin/books").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/book/list"));
    }

    @Test
    @DisplayName("ISBN 조회가 실패하면 오류 화면 대신 폼 위에 이유를 보여 주고 직접 입력하게 한다")
    void registerForm_조회실패_폼에안내() throws Exception {
        // given
        given(bookAdminService.lookupByIsbn(ISBN)).willThrow(new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE));
        given(bookAdminService.categoryOptions()).willReturn(List.of());

        // when, then
        mockMvc.perform(get("/admin/books/new").param("isbn", ISBN).with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/book/form"))
                .andExpect(model().attributeExists("lookupError"))
                .andExpect(content().string(containsString("외부 서비스에 연결할 수 없습니다")));
    }

    @Test
    @DisplayName("필수 값이 빠지면 등록하지 않고 폼에 필드 오류를 보여 준다")
    void register_필수값누락_폼오류() throws Exception {
        // when, then
        mockMvc.perform(post("/admin/books").with(admin()).with(csrf()).param("isbn", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/book/form"))
                .andExpect(model().attributeHasFieldErrors("bookForm", "isbn", "title", "author"));
        verify(bookAdminService, never()).register(any());
    }

    @Test
    @DisplayName("등록에 성공하면 상세 화면으로 보내고 결과 메시지를 한 번 보여 준다 (PRG)")
    void register_성공_상세로리다이렉트() throws Exception {
        // given
        given(bookAdminService.register(any(BookForm.class))).willReturn(10L);

        // when, then
        mockMvc.perform(post("/admin/books").with(admin()).with(csrf())
                        .param("isbn", ISBN).param("title", "이펙티브 자바").param("author", "조슈아 블로크 지음"))
                .andExpect(redirectedUrl("/admin/books/10"))
                .andExpect(flash().attribute("message", "도서를 등록했습니다."));
    }

    @Test
    @DisplayName("이미 등록된 ISBN이면 폼에 전역 오류로 보여 준다")
    void register_중복ISBN_폼전역오류() throws Exception {
        // given
        given(bookAdminService.register(any(BookForm.class))).willThrow(new BusinessException(ErrorCode.DUPLICATE_ISBN));

        // when, then
        mockMvc.perform(post("/admin/books").with(admin()).with(csrf())
                        .param("isbn", ISBN).param("title", "이펙티브 자바").param("author", "조슈아 블로크 지음"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 등록된 ISBN입니다.")));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 등록 요청을 보내면 처리하지 않고 로그인 화면으로 보낸다")
    void register_CSRF없음_처리안함() throws Exception {
        mockMvc.perform(post("/admin/books").with(admin()).param("isbn", ISBN))
                .andExpect(redirectedUrl("/admin/login?expired"));
        then(bookAdminService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("없는 도서 상세를 열면 404와 오류 화면을 보여 준다")
    void detail_없는도서_404화면() throws Exception {
        // given
        given(bookAdminService.getBook(999L)).willThrow(new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // when, then
        mockMvc.perform(get("/admin/books/999").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/business"));
    }

    @Test
    @DisplayName("권을 추가하면 상세 화면으로 돌아가 추가한 권 수를 알려 준다")
    void addCopies_성공_상세로() throws Exception {
        // given
        given(bookAdminService.addCopies(10L, 3)).willReturn(3);

        // when, then
        mockMvc.perform(post("/admin/books/10/copies").with(admin()).with(csrf()).param("quantity", "3"))
                .andExpect(redirectedUrl("/admin/books/10"))
                .andExpect(flash().attribute("message", "소장 자료 3권을 추가했습니다."));
    }

    @Test
    @DisplayName("상태 변경이 허용되지 않으면 오류 화면 대신 상세 화면에 이유를 보여 준다")
    void changeCopyStatus_허용안됨_상세에안내() throws Exception {
        // given
        given(bookAdminService.changeCopyStatus(1L, com.ryu.library.admin.book.dto.CopyStatusAction.LOST))
                .willThrow(new BusinessException(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED));

        // when, then
        mockMvc.perform(post("/admin/books/copies/1/status").with(admin()).with(csrf())
                        .param("action", "LOST").param("bookId", "10"))
                .andExpect(redirectedUrl("/admin/books/10"))
                .andExpect(flash().attribute("errorMessage", "현재 상태에서는 바꿀 수 없습니다."));
    }
}
