package com.ryu.ebook.user.ebook.dto;

import com.ryu.ebook.ebook.domain.Ebook;

public record EbookSummaryResponse(Long id, String title, String author, String coverImageUrl, long price,
        long salePrice, int discountRate, String categoryName, boolean onSale) {

    public static EbookSummaryResponse of(Ebook ebook, String categoryName) {
        return new EbookSummaryResponse(ebook.getId(), ebook.getTitle(), ebook.getAuthor(), ebook.getCoverImageUrl(),
                ebook.getPrice(), ebook.getSalePrice(), ebook.discountRate(), categoryName, ebook.isOnSale());
    }
}
