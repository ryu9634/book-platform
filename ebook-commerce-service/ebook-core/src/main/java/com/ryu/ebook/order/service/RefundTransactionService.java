package com.ryu.ebook.order.service;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.global.policy.EbookPolicy;
import com.ryu.ebook.order.domain.OrderItem;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.payment.domain.Payment;
import com.ryu.ebook.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundTransactionService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final BookshelfRepository bookshelfRepository;
    private final EbookRepository ebookRepository;
    private final EbookPolicy policy;
    private final Clock clock;

    // 회원 환불 조건: 본인 주문, 결제 완료, 결제 후 정해진 기간 이내, 아직 환불하지 않은 항목, 읽은 기록 없음
    @Transactional(readOnly = true)
    public RefundTarget prepareMemberRefund(Long memberId, String orderNo, Long orderItemId) {
        Orders order = ordersRepository.findByOrderNo(orderNo)
                .filter(o -> o.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderItem item = validateMemberRefundable(order, orderItemId);
        return new RefundTarget(approvedPayment(order).getPgTid(), item.getPriceSnapshot(), List.of(orderItemId));
    }

    // 관리자 강제 환불: 결제 완료 주문의 남은 항목 전부. 기간과 열람 조건을 보지 않습니다.
    @Transactional(readOnly = true)
    public RefundTarget prepareAdminRefund(String orderNo) {
        Orders order = ordersRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "결제 완료된 주문만 환불할 수 있습니다.");
        }
        List<OrderItem> remaining = order.getItems().stream().filter(i -> !i.isRefunded()).toList();
        long amount = remaining.stream().mapToLong(OrderItem::getPriceSnapshot).sum();
        return new RefundTarget(approvedPayment(order).getPgTid(), amount, remaining.stream().map(OrderItem::getId).toList());
    }

    // 주문을 잠그고 조건을 다시 확인한 뒤 반영합니다. 같은 항목을 동시에 두 번 환불하지 못하게 합니다.
    @Transactional
    public void apply(String orderNo, Collection<Long> orderItemIds, boolean checkMemberRules) {
        LocalDateTime now = LocalDateTime.now(clock);
        Orders order = ordersRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        for (Long orderItemId : orderItemIds) {
            if (checkMemberRules) {
                validateMemberRefundable(order, orderItemId);
            }
            OrderItem item = order.refundItem(orderItemId);
            bookshelfRepository.findByOrderItemId(orderItemId).ifPresent(Bookshelf::deactivate);
            ebookRepository.findById(item.getEbookId()).ifPresent(ebook -> ebook.decreaseSales());
        }
        if (order.getStatus() == OrderStatus.REFUNDED) {
            approvedPayment(order).cancel(now);
        }
        log.info("order refunded. orderNo={}, items={}, orderStatus={}", orderNo, orderItemIds, order.getStatus());
    }

    private OrderItem validateMemberRefundable(Orders order, Long orderItemId) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "결제 완료된 주문만 환불할 수 있습니다.");
        }
        if (order.getPaidAt().plusDays(policy.refundDays()).isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "결제 후 " + policy.refundDays() + "일이 지나 환불할 수 없습니다.");
        }
        OrderItem item = order.getItems().stream().filter(i -> i.getId().equals(orderItemId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (item.isRefunded()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "이미 환불한 항목입니다.");
        }
        if (bookshelfRepository.findByOrderItemId(orderItemId).filter(Bookshelf::hasBeenRead).isPresent()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "읽은 전자책은 환불할 수 없습니다.");
        }
        return item;
    }

    private Payment approvedPayment(Orders order) {
        return paymentRepository.findByOrderId(order.getId())
                .filter(payment -> payment.isApproved() || order.getStatus() == OrderStatus.REFUNDED)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "승인된 결제가 없습니다."));
    }

    public record RefundTarget(String pgTid, long amount, List<Long> orderItemIds) {
    }
}
