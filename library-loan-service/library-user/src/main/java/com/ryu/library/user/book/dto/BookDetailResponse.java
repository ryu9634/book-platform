package com.ryu.library.user.book.dto;

import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import java.time.LocalDate;
import java.util.List;

public record BookDetailResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        LocalDate publishedDate,
        String description,
        String coverImageUrl,
        String categoryName,
        List<CopyItem> copies) {

    public static BookDetailResponse of(Book book, String categoryName, List<BookCopy> copies) {
        List<CopyItem> items = copies.stream()
                .filter(copy -> copy.getStatus() != BookCopyStatus.LOST && copy.getStatus() != BookCopyStatus.DISCARDED)
                .map(CopyItem::from)
                .toList();
        return new BookDetailResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getPublishedDate(), book.getDescription(), book.getCoverImageUrl(),
                categoryName, items);
    }

    public long availableCount() {
        return copies.stream().filter(copy -> copy.status() == BookCopyStatus.AVAILABLE).count();
    }

    public record CopyItem(Long id, String barcode, BookCopyStatus status) {

        static CopyItem from(BookCopy copy) {
            return new CopyItem(copy.getId(), copy.getBarcode(), copy.getStatus());
        }

        public String statusLabel() {
            return switch (status) {
                case AVAILABLE -> "대출 가능";
                case ON_LOAN -> "대출 중";
                case RESERVED -> "예약 보관";
                case LOST, DISCARDED -> "이용 불가";
            };
        }
    }
}
