package com.ryu.library.book;

import com.ryu.library.book.domain.Book;
import java.time.LocalDate;

public final class BookFixture {

    private BookFixture() {
    }

    public static Book book(String isbn, String title) {
        return Book.builder()
                .isbn(isbn)
                .title(title)
                .author("저자")
                .publisher("출판사")
                .publishedDate(LocalDate.of(2024, 1, 1))
                .build();
    }
}
