package com.ryu.library.user.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.user.book.dto.BookSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookQueryServiceTest {

    @Mock
    BookRepository bookRepository;

    @Mock
    BookCopyRepository bookCopyRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    BookQueryService bookQueryService;

    private static BookCopyRepository.CopyCount row(Long bookId, BookCopyStatus status, long count) {
        return new BookCopyRepository.CopyCount() {
            public Long getBookId() { return bookId; }
            public BookCopyStatus getStatus() { return status; }
            public long getCount() { return count; }
        };
    }

    @Test
    @DisplayName("목록의 대출 가능 권 수와 소장 권 수를 한 번의 집계 결과로 채운다 (분실, 폐기는 소장에서 제외)")
    void search_권수집계_반영() {
        // given
        Book a = BookFixture.book("9780000000031", "A");
        Book b = BookFixture.book("9780000000032", "B");
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);
        given(bookRepository.search(any(), any(), any())).willReturn(new PageImpl<>(List.of(a, b), PageRequest.of(0, 20), 2));
        given(bookCopyRepository.countByBookIdsGroupByStatus(anyCollection())).willReturn(List.of(
                row(1L, BookCopyStatus.AVAILABLE, 2), row(1L, BookCopyStatus.ON_LOAN, 1), row(1L, BookCopyStatus.LOST, 4),
                row(2L, BookCopyStatus.ON_LOAN, 1)));

        // when
        Page<BookSummaryResponse> page = bookQueryService.search("  ", null, PageRequest.of(0, 20));

        // then
        assertThat(page.getContent()).extracting(r -> r.availableCount() + "/" + r.holdingCount())
                .containsExactly("2/3", "0/1");
        assertThat(page.getContent().get(1).isAvailable()).isFalse();
    }
}
