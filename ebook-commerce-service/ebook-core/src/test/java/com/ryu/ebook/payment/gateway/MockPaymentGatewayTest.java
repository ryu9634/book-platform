package com.ryu.ebook.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    @DisplayName("금액 끝자리가 7이거나 실패 시뮬레이션이면 거절하고, 같은 주문번호는 한 번만 승인한다")
    void 실패재현_멱등성() {
        assertThat(gateway.approve("A", 9007, false).success()).isFalse();
        assertThat(gateway.approve("A", 9000, true).success()).isFalse();
        assertThat(gateway.approve("A", 9000, false).success()).isTrue();
        assertThat(gateway.approve("A", 9000, false).success()).isFalse();
    }
}
