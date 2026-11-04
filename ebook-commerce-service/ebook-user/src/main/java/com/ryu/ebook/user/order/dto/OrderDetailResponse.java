package com.ryu.ebook.user.order.dto;

import com.ryu.ebook.order.domain.OrderItemStatus;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.payment.domain.Payment;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        String orderNo,
        OrderStatus status,
        long totalAmount,
        long discountAmount,
        long paidAmount,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        List<Item> items,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String failReason) {

    public static OrderDetailResponse of(Orders order, Payment payment, java.util.function.Predicate<Long> refundable) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getId(), i.getEbookId(), i.getTitleSnapshot(), i.getPriceSnapshot(), i.getStatus(),
                        refundable.test(i.getId())))
                .toList();
        return new OrderDetailResponse(order.getOrderNo(), order.getStatus(), order.getTotalAmount(),
                order.getDiscountAmount(), order.getPaidAmount(), order.getOrderedAt(), order.getPaidAt(), items,
                payment == null ? null : payment.getMethod(), payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getFailReason());
    }

    public boolean pending() {
        return status == OrderStatus.PENDING;
    }

    public String statusLabel() {
        return label(status);
    }

    static String label(OrderStatus status) {
        return switch (status) {
            case PENDING -> "결제 대기";
            case PAID -> "결제 완료";
            case CANCELED -> "주문 취소";
            case REFUNDED -> "환불 완료";
        };
    }

    public record Item(Long orderItemId, Long ebookId, String title, long price, OrderItemStatus status, boolean refundable) {
    }
}
