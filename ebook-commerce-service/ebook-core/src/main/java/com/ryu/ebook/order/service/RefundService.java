package com.ryu.ebook.order.service;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.payment.gateway.PaymentGateway;
import com.ryu.ebook.payment.gateway.PaymentGateway.PgResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 환불 흐름. 결제 승인과 같은 방식입니다: 짧은 읽기 트랜잭션으로 확인 → 트랜잭션 밖에서 PG 부분 취소 → 주문을 잠그고 반영
// 회원 환불(조건 확인)과 관리자 강제 환불(조건 없이 남은 항목 전부)이 함께 씁니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundTransactionService transactionService;
    private final PaymentGateway paymentGateway;

    public void refundByMember(Long memberId, String orderNo, Long orderItemId) {
        RefundTransactionService.RefundTarget target = transactionService.prepareMemberRefund(memberId, orderNo, orderItemId);
        cancelAtPg(target);
        transactionService.apply(orderNo, List.of(orderItemId), true);
    }

    public void refundAllByAdmin(String orderNo) {
        RefundTransactionService.RefundTarget target = transactionService.prepareAdminRefund(orderNo);
        cancelAtPg(target);
        transactionService.apply(orderNo, target.orderItemIds(), false);
    }

    // PG 취소가 실패하면 DB는 건드리지 않습니다. 돈이 돌아가지 않았는데 소유권만 사라지는 일을 막기 위해서입니다.
    private void cancelAtPg(RefundTransactionService.RefundTarget target) {
        PgResult result = paymentGateway.cancel(target.pgTid(), target.amount());
        if (!result.success()) {
            log.warn("pg cancel failed. reason={}", result.failReason());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
    }
}
