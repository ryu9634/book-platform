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
import com.ryu.library.admin.book.dto.CopyStatusAction;
import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class BookAdminServiceTest {

    private static final String ISBN = "9788966262281";

    @Mock
    BookRepository bookRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BookCopyRepository bookCopyRepository;

    @Mock
    NationalLibraryClient nationalLibraryClient;

    @InjectMocks
    BookAdminService bookAdminService;

    @Test
    @DisplayName("ISBN을 조회하면 국립중앙도서관 정보로 등록 폼을 채운다")
    void lookupByIsbn_조회성공_폼채움() {
        // given
        given(nationalLibraryClient.findByIsbn(ISBN)).willReturn(Optional.of(new NlBookInfo(
                ISBN, "이펙티브 자바", "조슈아 블로크 지음 ; 개앞맵시 옮김", "인사이트", 2018, null, "005.133")));

        // when
        BookForm form = bookAdminService.lookupByIsbn(ISBN);

        // then: 발행일은 연도만 있어 날짜는 비워 두고 연도는 참고용으로만 둔다
        assertThat(form.getTitle()).isEqualTo("이펙티브 자바");
        assertThat(form.getIsbn()).isEqualTo(ISBN);
        assertThat(form.getPublishedDate()).isNull();
        assertThat(form.getPublishedYearHint()).isEqualTo(2018);
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
        form.setTitle("이펙티브 자바");
        form.setAuthor("조슈아 블로크 지음");
        form.setCategoryId(1L);
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(bookRepository.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        bookAdminService.register(form);

        // then
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("이펙티브 자바");
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

    @Test
    @DisplayName("권을 추가하면 이미 있는 권 수 다음 번호부터 바코드를 만든다")
    void addCopies_기존2권_3번부터() {
        // given
        Book book = BookFixture.book(ISBN, "이펙티브 자바");
        given(bookRepository.findById(7L)).willReturn(Optional.of(book));
        given(bookCopyRepository.countByBookId(7L)).willReturn(2L);

        // when
        bookAdminService.addCopies(7L, 2);

        // then
        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(BookCopy::getBarcode)
                .containsExactly("LIB-000007-003", "LIB-000007-004");
        assertThat(captor.getAllValues()).extracting(BookCopy::getStatus).containsOnly(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("한 번에 추가할 수 있는 권 수는 1~10권이다")
    void addCopies_수량범위밖_C001() {
        assertThatThrownBy(() -> bookAdminService.addCopies(7L, 0))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> bookAdminService.addCopies(7L, 11))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    @DisplayName("대출 중인 권을 분실 처리하려 하면 거부한다")
    void changeCopyStatus_대출중분실_B009() {
        // given
        BookCopy copy = BookCopy.create(BookFixture.book(ISBN, "제목"), "LIB-000001-001");
        copy.lend();
        given(bookCopyRepository.findById(1L)).willReturn(Optional.of(copy));

        // when, then
        assertThatThrownBy(() -> bookAdminService.changeCopyStatus(1L, CopyStatusAction.LOST))
                .extracting("errorCode").isEqualTo(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("도서를 수정하면 ISBN은 그대로 두고 서지 정보만 바꾼다")
    void update_서지정보변경_ISBN유지() {
        // given
        Book book = BookFixture.book(ISBN, "옛 제목");
        given(bookRepository.findById(7L)).willReturn(Optional.of(book));
        BookForm form = BookForm.withIsbn("9790000000000");
        form.setTitle("새 제목");
        form.setAuthor("새 저자");

        // when
        bookAdminService.update(7L, form);

        // then
        assertThat(book.getTitle()).isEqualTo("새 제목");
        assertThat(book.getIsbn()).isEqualTo(ISBN);
    }
}
