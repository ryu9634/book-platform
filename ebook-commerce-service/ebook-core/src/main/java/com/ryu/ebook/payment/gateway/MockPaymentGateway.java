package com.ryu.ebook.payment.gateway;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 학습용 가짜 PG. 실제 PG처럼 같은 주문번호를 두 번 승인하지 않습니다(멱등성).
// 실패 재현: 결제 화면의 "실패 시뮬레이션" 또는 금액의 1원 자리가 7이면 실패합니다.
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final Map<String, String> approvedTidByOrderNo = new ConcurrentHashMap<>();

    @Override
    public PgResult approve(String orderNo, long amount, boolean simulateFailure) {
        if (simulateFailure || amount % 10 == 7) {
            log.info("mock pg declined. orderNo={}", orderNo);
            return PgResult.fail("카드사 승인이 거절되었습니다. (테스트 실패)");
        }
        String tid = "MOCK-" + UUID.randomUUID();
        // putIfAbsent: 같은 주문번호로 동시에 두 번 승인 요청이 와도 하나만 승인됩니다.
        if (approvedTidByOrderNo.putIfAbsent(orderNo, tid) != null) {
            return PgResult.fail("이미 승인된 주문입니다.");
        }
        log.info("mock pg approved. orderNo={}, tid={}", orderNo, tid);
        return PgResult.ok(tid);
    }

    @Override
    public PgResult cancel(String pgTid, long amount) {
        log.info("mock pg canceled. tid={}, amount={}", pgTid, amount);
        return PgResult.ok(pgTid);
    }
}
