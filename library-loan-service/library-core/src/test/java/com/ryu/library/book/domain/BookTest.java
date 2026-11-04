package com.ryu.library.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookTest {

    @Test
    @DisplayName("외부 데이터가 컬럼 길이보다 길면 컬럼 길이에 맞춰 자르고 말줄임표를 붙인다")
    void builder_긴값_컬럼길이에맞춤() {
        // given
        String longAuthor = "가".repeat(150);

        // when
        Book book = Book.builder().title("제목").author(longAuthor).build();

        // then
        assertThat(book.getAuthor()).hasSize(Book.AUTHOR_MAX).endsWith("…");
        assertThat(book.getTitle()).isEqualTo("제목");
    }
}
