package com.ryu.ebook.user.ebook.dto;

import com.ryu.ebook.ebook.domain.Ebook;
import java.time.LocalDate;

public record EbookDetailResponse(Long id, String isbn, String title, String author, String publisher,
        String description, String coverImageUrl, long price, long salePrice, int discountRate, boolean onSale,
        Integer pageCount, LocalDate publishedDate, String categoryName) {

    public static EbookDetailResponse of(Ebook ebook, String categoryName) {
        return new EbookDetailResponse(ebook.getId(), ebook.getIsbn(), ebook.getTitle(), ebook.getAuthor(),
                ebook.getPublisher(), ebook.getDescription(), ebook.getCoverImageUrl(), ebook.getPrice(),
                ebook.getSalePrice(), ebook.discountRate(), ebook.isOnSale(), ebook.getPageCount(),
                ebook.getPublishedDate(), categoryName);
    }
}
