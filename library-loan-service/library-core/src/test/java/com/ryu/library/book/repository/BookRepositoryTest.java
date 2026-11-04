package com.ryu.library.book.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.config.ClockConfig;
import com.ryu.library.global.config.JpaConfig;
import com.ryu.library.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class BookRepositoryTest {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Test
    @DisplayName("제목에 검색어가 들어간 도서를 페이지 단위로 찾는다")
    void findByTitleContaining_검색어_페이징() {
        // given
        bookRepository.save(BookFixture.book("9780000000001", "자바의 정석"));
        bookRepository.save(BookFixture.book("9780000000002", "모던 자바 인 액션"));
        bookRepository.saveAndFlush(BookFixture.book("9780000000003", "파이썬 입문"));

        // when
        Page<Book> page = bookRepository.findByTitleContaining("자바", PageRequest.of(0, 1));

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(bookRepository.existsByIsbn("9780000000003")).isTrue();
    }

    @Test
    @DisplayName("기본 분류는 마이그레이션(V2)으로 들어간 순서대로 조회된다")
    void categories_기본분류_정렬순() {
        assertThat(categoryRepository.findAllByOrderBySortOrderAsc())
                .extracting("name")
                .containsExactly("문학", "컴퓨터/IT", "경제/경영");
    }

    @Test
    @DisplayName("검색어는 제목이나 저자에서 대소문자 없이 찾고, 분류가 있으면 분류로도 거른다")
    void search_제목저자_분류() {
        // given
        bookRepository.save(Book.builder().isbn("9780000000021").title("Effective Java").author("Joshua Bloch").categoryId(1L).build());
        bookRepository.save(Book.builder().isbn("9780000000022").title("자바 입문").author("홍길동").categoryId(2L).build());
        bookRepository.saveAndFlush(Book.builder().isbn("9780000000023").title("파이썬").author("JAVA 전문가").categoryId(1L).build());

        // when, then
        assertThat(bookRepository.search("java", null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(bookRepository.search("자바", null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(bookRepository.search(null, 1L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(bookRepository.search("java", 2L, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(bookRepository.search(null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }
}
