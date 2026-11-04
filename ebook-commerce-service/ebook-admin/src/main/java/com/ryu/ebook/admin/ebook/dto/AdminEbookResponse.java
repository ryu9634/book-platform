package com.ryu.ebook.admin.ebook.dto;

import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import java.time.LocalDate;

public record AdminEbookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        String description,
        String coverImageUrl,
        long price,
        long salePrice,
        int discountRate,
        EbookStatus status,
        LocalDate publishedDate,
        long salesCount,
        boolean hasFile,
        Integer pageCount,
        String categoryName) {

    public static AdminEbookResponse of(Ebook ebook, String categoryName) {
        return new AdminEbookResponse(ebook.getId(), ebook.getIsbn(), ebook.getTitle(), ebook.getAuthor(),
                ebook.getPublisher(), ebook.getDescription(), ebook.getCoverImageUrl(), ebook.getPrice(),
                ebook.getSalePrice(), ebook.discountRate(), ebook.getStatus(), ebook.getPublishedDate(),
                ebook.getSalesCount(), ebook.hasFile(), ebook.getPageCount(), categoryName);
    }

    public String statusLabel() {
        return switch (status) {
            case ON_SALE -> "판매 중";
            case STOP_SALE -> "판매 중지";
            case HIDDEN -> "숨김";
        };
    }
}
