package com.ryu.library.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.global.config.ClockConfig;
import com.ryu.library.global.config.JpaConfig;
import com.ryu.library.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class BookCopyRepositoryTest {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookCopyRepository bookCopyRepository;

    @Test
    @DisplayName("여러 도서의 상태별 권 수를 GROUP BY 한 번으로 센다")
    void countByBookIdsGroupByStatus_한번에집계() {
        // given
        Book a = bookRepository.save(BookFixture.book("9780000000011", "A"));
        Book b = bookRepository.save(BookFixture.book("9780000000012", "B"));
        bookCopyRepository.save(BookCopy.create(a, "LIB-A-1"));
        BookCopy lent = BookCopy.create(a, "LIB-A-2");
        lent.lend();
        bookCopyRepository.save(lent);
        bookCopyRepository.saveAndFlush(BookCopy.create(b, "LIB-B-1"));

        // when
        List<BookCopyRepository.CopyCount> counts =
                bookCopyRepository.countByBookIdsGroupByStatus(List.of(a.getId(), b.getId()));

        // then
        assertThat(counts).extracting(c -> c.getBookId() + ":" + c.getStatus() + ":" + c.getCount())
                .containsExactlyInAnyOrder(
                        a.getId() + ":" + BookCopyStatus.AVAILABLE + ":1",
                        a.getId() + ":" + BookCopyStatus.ON_LOAN + ":1",
                        b.getId() + ":" + BookCopyStatus.AVAILABLE + ":1");
    }

    @Test
    @DisplayName("같은 바코드는 두 번 저장할 수 없다 (UNIQUE 제약)")
    void save_중복바코드_제약위반() {
        // given
        Book book = bookRepository.save(BookFixture.book("9780000000013", "C"));
        bookCopyRepository.saveAndFlush(BookCopy.create(book, "LIB-DUP"));

        // when, then
        assertThatThrownBy(() -> bookCopyRepository.saveAndFlush(BookCopy.create(book, "LIB-DUP")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
