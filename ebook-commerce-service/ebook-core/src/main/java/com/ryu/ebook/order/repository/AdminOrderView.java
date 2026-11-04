package com.ryu.ebook.order.repository;

import com.ryu.ebook.order.domain.OrderStatus;
import java.time.LocalDateTime;

public record AdminOrderView(String orderNo, Long memberId, String loginId, String memberName, OrderStatus status,
        long paidAmount, LocalDateTime orderedAt, LocalDateTime paidAt) {
}
