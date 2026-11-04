package com.ryu.ebook.user.cart.dto;

import java.util.List;

public record CartResponse(List<Item> items, long totalPrice, long totalSalePrice) {

    public static CartResponse of(List<Item> items) {
        return new CartResponse(items,
                items.stream().mapToLong(Item::price).sum(),
                items.stream().filter(Item::onSale).mapToLong(Item::salePrice).sum());
    }

    public long discount() {
        return totalPrice - totalSalePrice;
    }

    // 판매가 중지된 전자책은 장바구니에 남아 있어도 주문 합계에서 뺍니다.
    public record Item(Long cartItemId, Long ebookId, String title, String author, String coverImageUrl, long price,
            long salePrice, boolean onSale) {
    }
}
