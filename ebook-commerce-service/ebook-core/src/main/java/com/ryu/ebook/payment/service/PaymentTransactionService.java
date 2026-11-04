package com.ryu.ebook.payment.service;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.order.domain.OrderItem;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.payment.domain.Payment;
import com.ryu.ebook.payment.gateway.PaymentGateway.PgResult;
import com.ryu.ebook.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 결제 승인의 DB 반영 부분. PG 호출은 이 클래스 밖(PaymentService)에서 트랜잭션 없이 합니다.
// 같은 클래스 안에서 부르면 @Transactional 프록시를 거치지 않으므로 별도 빈으로 나눕니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final BookshelfRepository bookshelfRepository;
    private final EbookRepository ebookRepository;
    private final Clock clock;

    // 승인 전 확인: 본인 주문이고 결제 대기 중인지. 결제할 금액을 돌려줍니다.
    @Transactional(readOnly = true)
    public long prepare(Long memberId, String orderNo) {
        Orders order = ordersRepository.findByOrderNo(orderNo)
                .filter(o -> o.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isPending()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }
        return order.getPaidAmount();
    }

    // PG 결과를 반영합니다. 주문 행을 잠근 뒤 상태를 다시 확인하므로 같은 주문에 승인이 두 번 반영되지 않습니다.
    // 반환값: 승인이 반영되었으면 true. 주문이 이미 결제 대기가 아니면 예외(호출한 쪽이 PG 승인을 취소).
    @Transactional
    public boolean apply(String orderNo, long requestedAmount, PgResult result) {
        LocalDateTime now = LocalDateTime.now(clock);
        Orders order = ordersRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isPending()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!result.success()) {
            payment.fail(result.failReason());
            log.info("payment failed. orderNo={}, reason={}", orderNo, result.failReason());
            return false;
        }
        if (requestedAmount != order.getPaidAmount() || payment.getAmount() != order.getPaidAmount()) {
            payment.fail("결제 금액이 주문 금액과 다릅니다.");
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        payment.approve(result.tid(), now);
        order.pay(now);
        for (OrderItem item : order.getItems()) {
            bookshelfRepository.findByMemberIdAndEbookId(order.getMemberId(), item.getEbookId())
                    .ifPresentOrElse(
                            shelf -> shelf.reacquire(item.getId(), now),
                            () -> bookshelfRepository.save(Bookshelf.acquire(order.getMemberId(), item.getEbookId(), item.getId(), now)));
            ebookRepository.findById(item.getEbookId()).ifPresent(ebook -> ebook.increaseSales());
        }
        log.info("payment approved. orderNo={}, tid={}", orderNo, result.tid());
        return true;
    }
}
