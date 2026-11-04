package com.ryu.ebook.payment.service;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.payment.gateway.PaymentGateway;
import com.ryu.ebook.payment.gateway.PaymentGateway.PgResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 결제 승인 흐름. 이 클래스에는 @Transactional을 붙이지 않습니다.
// 1) 짧은 읽기 트랜잭션으로 금액 확인 → 2) 트랜잭션 밖에서 PG 호출 → 3) 주문을 잠그고 결과 반영
// PG 호출을 트랜잭션 안에서 하면 PG가 느릴 때 DB 커넥션과 잠금을 그동안 계속 잡고 있게 됩니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionService transactionService;
    private final PaymentGateway paymentGateway;

    public PaymentOutcome approve(Long memberId, String orderNo, boolean simulateFailure) {
        long amount = transactionService.prepare(memberId, orderNo);
        PgResult result = paymentGateway.approve(orderNo, amount, simulateFailure);
        try {
            boolean approved = transactionService.apply(orderNo, amount, result);
            return approved ? PaymentOutcome.success() : PaymentOutcome.failure(result.failReason());
        } catch (BusinessException e) {
            // PG는 승인했는데 DB에 반영하지 못했으면(그 사이 주문 취소 등) PG 승인을 취소합니다(보상 작업).
            if (result.success()) {
                paymentGateway.cancel(result.tid(), amount);
                log.warn("pg approval compensated. orderNo={}, reason={}", orderNo, e.getErrorCode().getCode());
            }
            throw e;
        }
    }

    public record PaymentOutcome(boolean approved, String failReason) {

        static PaymentOutcome success() {
            return new PaymentOutcome(true, null);
        }

        static PaymentOutcome failure(String reason) {
            return new PaymentOutcome(false, reason);
        }
    }
}
