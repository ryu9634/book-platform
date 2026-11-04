package com.ryu.ebook.user.order.dto;

import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import java.time.LocalDateTime;

public record OrderSummaryResponse(String orderNo, OrderStatus status, long paidAmount, LocalDateTime orderedAt,
        String title, int itemCount) {

    public static OrderSummaryResponse from(Orders order) {
        var items = order.getItems();
        String title = items.isEmpty() ? "" : items.get(0).getTitleSnapshot() + (items.size() > 1 ? " 외 " + (items.size() - 1) + "권" : "");
        return new OrderSummaryResponse(order.getOrderNo(), order.getStatus(), order.getPaidAmount(), order.getOrderedAt(),
                title, items.size());
    }

    public String statusLabel() {
        return OrderDetailResponse.label(status);
    }
}
