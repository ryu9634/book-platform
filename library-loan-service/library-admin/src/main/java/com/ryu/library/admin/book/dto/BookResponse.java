package com.ryu.library.admin.book.dto;

import com.ryu.library.book.domain.Book;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        LocalDate publishedDate,
        String description,
        String coverImageUrl,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt) {

    public static BookResponse of(Book book, String categoryName) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                book.getPublishedDate(), book.getDescription(), book.getCoverImageUrl(), book.getCategoryId(),
                categoryName, book.getCreatedAt());
    }
}
