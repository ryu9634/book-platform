package com.ryu.ebook.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrdersTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);

    private Orders order() {
        Orders order = Orders.create("20261104-000001", 1L, NOW,
                List.of(new Orders.Line(10L, "A", 9000), new Orders.Line(11L, "B", 12000)));
        ReflectionTestUtils.setField(order.getItems().get(0), "id", 100L);
        ReflectionTestUtils.setField(order.getItems().get(1), "id", 101L);
        return order;
    }

    @Test
    @DisplayName("주문 금액은 항목 판매가 스냅샷의 합이고, 빈 주문은 만들 수 없다")
    void create_합계_빈주문() {
        assertThat(order().getPaidAmount()).isEqualTo(21000);
        assertThatThrownBy(() -> Orders.create("x", 1L, NOW, List.of()))
                .extracting("errorCode").isEqualTo(ErrorCode.EMPTY_ORDER);
    }

    @Test
    @DisplayName("결제 대기 주문만 결제·취소할 수 있다")
    void pay_cancel_결제대기만() {
        Orders order = order();
        order.pay(NOW);
        assertThatThrownBy(() -> order.cancel(NOW)).extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_PENDING);
        assertThatThrownBy(() -> order.pay(NOW)).extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_PENDING);
    }

    @Test
    @DisplayName("항목을 하나씩 환불하고, 모든 항목이 환불되면 주문이 환불 상태가 된다")
    void refundItem_부분_전체() {
        Orders order = order();
        order.pay(NOW);

        order.refundItem(100L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        order.refundItem(101L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThatThrownBy(() -> order.refundItem(101L)).extracting("errorCode").isEqualTo(ErrorCode.REFUND_NOT_ALLOWED);
    }
}
