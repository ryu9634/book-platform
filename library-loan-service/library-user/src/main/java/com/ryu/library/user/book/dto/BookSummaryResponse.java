package com.ryu.library.user.book.dto;

import com.ryu.library.book.domain.Book;

// 목록용. 대출 가능 권 수와 소장 권 수(분실, 폐기 제외)를 함께 보여 줍니다.
public record BookSummaryResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String coverImageUrl,
        String categoryName,
        long availableCount,
        long holdingCount) {

    public static BookSummaryResponse of(Book book, String categoryName, CopyCounts counts) {
        return new BookSummaryResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(),
                book.getCoverImageUrl(), categoryName, counts.available(), counts.holding());
    }

    public boolean isAvailable() {
        return availableCount > 0;
    }
}
