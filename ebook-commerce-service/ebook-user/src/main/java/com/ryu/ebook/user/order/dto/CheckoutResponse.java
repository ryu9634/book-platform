package com.ryu.ebook.user.order.dto;

import com.ryu.ebook.ebook.domain.Ebook;
import java.util.List;

public record CheckoutResponse(List<Line> lines, long totalAmount) {

    public static CheckoutResponse of(List<Ebook> ebooks) {
        List<Line> lines = ebooks.stream().map(e -> new Line(e.getId(), e.getTitle(), e.getPrice(), e.getSalePrice())).toList();
        return new CheckoutResponse(lines, lines.stream().mapToLong(Line::salePrice).sum());
    }

    public record Line(Long ebookId, String title, long price, long salePrice) {
    }
}
