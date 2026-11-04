package com.ryu.ebook.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    @DisplayName("실패한 결제는 다시 승인할 수 있고, 승인된 결제는 실패로 덮어쓰지 않는다")
    void 재시도_덮어쓰기방지() {
        Payment payment = Payment.ready(1L, PaymentMethod.MOCK, 9000);
        payment.fail("거절");
        payment.approve("TID-1", LocalDateTime.now());

        payment.fail("늦게 도착한 실패");

        assertThat(payment.isApproved()).isTrue();
        assertThat(payment.getFailReason()).isNull();
    }
}
