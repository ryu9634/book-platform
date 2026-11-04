package com.ryu.library.admin.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.library.admin.book.client.NationalLibraryClient;
import com.ryu.library.admin.book.client.NlBookInfo;
import com.ryu.library.admin.book.dto.BookForm;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookAdminServiceTest {

    private static final String ISBN = "9788966262281";

    @Mock
    BookRepository bookRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    NationalLibraryClient nationalLibraryClient;

    @InjectMocks
    BookAdminService bookAdminService;

    @Test
    @DisplayName("ISBN을 조회하면 국립중앙도서관 정보로 등록 폼을 채운다")
    void lookupByIsbn_조회성공_폼채움() {
        // given
        given(nationalLibraryClient.findByIsbn(ISBN)).willReturn(Optional.of(new NlBookInfo(
                ISBN, "클린 코드", "로버트 C. 마틴", "인사이트", LocalDate.of(2013, 12, 24), null, null, "005.1")));

        // when
        BookForm form = bookAdminService.lookupByIsbn(ISBN);

        // then
        assertThat(form.getTitle()).isEqualTo("클린 코드");
        assertThat(form.getIsbn()).isEqualTo(ISBN);
    }

    @Test
    @DisplayName("이미 등록된 ISBN이면 외부 API를 부르지 않고 B002로 알린다")
    void lookupByIsbn_이미등록_B002() {
        // given
        given(bookRepository.existsByIsbn(ISBN)).willReturn(true);

        // when, then
        assertThatThrownBy(() -> bookAdminService.lookupByIsbn(ISBN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ISBN);
        verify(nationalLibraryClient, never()).findByIsbn(any());
    }

    @Test
    @DisplayName("국립중앙도서관에 없는 ISBN이면 B003으로 알린다")
    void lookupByIsbn_외부에없음_B003() {
        // given
        given(nationalLibraryClient.findByIsbn(ISBN)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> bookAdminService.lookupByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.ISBN_NOT_FOUND_IN_NL);
    }

    @Test
    @DisplayName("폼 내용으로 도서를 등록한다")
    void register_정상_저장() {
        // given
        BookForm form = BookForm.withIsbn(ISBN);
        form.setTitle("클린 코드");
        form.setAuthor("로버트 C. 마틴");
        form.setCategoryId(1L);
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        bookAdminService.register(form);

        // then
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("클린 코드");
        assertThat(captor.getValue().getCategoryId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("없는 분류를 고르면 등록하지 않는다 (FK가 없으므로 서비스에서 확인)")
    void register_없는분류_B005() {
        // given
        BookForm form = BookForm.withIsbn(ISBN);
        form.setCategoryId(999L);
        given(categoryRepository.existsById(999L)).willReturn(false);

        // when, then
        assertThatThrownBy(() -> bookAdminService.register(form))
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        verify(bookRepository, never()).save(any());
    }
}
