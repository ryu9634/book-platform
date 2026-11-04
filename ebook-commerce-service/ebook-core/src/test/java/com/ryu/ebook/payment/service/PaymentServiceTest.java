package com.ryu.ebook.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.payment.gateway.PaymentGateway;
import com.ryu.ebook.payment.gateway.PaymentGateway.PgResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private final PaymentTransactionService tx = mock(PaymentTransactionService.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final PaymentService service = new PaymentService(tx, gateway);

    @Test
    @DisplayName("PG는 승인했는데 그 사이 주문이 취소되어 반영하지 못하면 PG 승인을 취소한다 (보상 작업)")
    void approve_반영실패_PG취소() {
        given(tx.prepare(1L, "NO-1")).willReturn(9000L);
        given(gateway.approve("NO-1", 9000L, false)).willReturn(PgResult.ok("TID-1"));
        given(tx.apply(any(), anyLong(), any())).willThrow(new BusinessException(ErrorCode.ORDER_NOT_PENDING));

        assertThatThrownBy(() -> service.approve(1L, "NO-1", false))
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_PENDING);
        verify(gateway).cancel("TID-1", 9000L);
    }

    @Test
    @DisplayName("PG가 거절하면 실패 사유를 돌려주고 취소할 승인은 없다")
    void approve_PG거절() {
        given(tx.prepare(1L, "NO-1")).willReturn(9000L);
        given(gateway.approve("NO-1", 9000L, true)).willReturn(PgResult.fail("거절"));
        given(tx.apply(any(), anyLong(), any())).willReturn(false);

        PaymentService.PaymentOutcome outcome = service.approve(1L, "NO-1", true);

        assertThat(outcome.approved()).isFalse();
        assertThat(outcome.failReason()).isEqualTo("거절");
        verify(gateway, never()).cancel(any(), anyLong());
    }
}
