package com.ryu.ebook.ebook;

import com.ryu.ebook.ebook.domain.Ebook;

public final class EbookFixture {

    private EbookFixture() {
    }

    public static Ebook ebook(String isbn, String title, long price, long salePrice) {
        return Ebook.builder().isbn(isbn).title(title).author("저자").publisher("출판사")
                .price(price).salePrice(salePrice).build();
    }
}
