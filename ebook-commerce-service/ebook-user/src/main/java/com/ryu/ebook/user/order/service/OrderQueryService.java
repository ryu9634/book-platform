package com.ryu.ebook.user.order.service;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.global.policy.EbookPolicy;
import com.ryu.ebook.order.domain.OrderItem;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.order.service.OrderService;
import com.ryu.ebook.payment.repository.PaymentRepository;
import com.ryu.ebook.user.order.dto.CheckoutResponse;
import com.ryu.ebook.user.order.dto.OrderDetailResponse;
import com.ryu.ebook.user.order.dto.OrderSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderService orderService;
    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final BookshelfRepository bookshelfRepository;
    private final EbookPolicy policy;
    private final Clock clock;

    public CheckoutResponse checkout(Long memberId, Collection<Long> ebookIds) {
        return CheckoutResponse.of(orderService.orderableEbooks(memberId, ebookIds));
    }

    // 주문 목록의 항목(컬렉션)은 fetch join 대신 default_batch_fetch_size로 한 번에 가져옵니다(페이징과 컬렉션 fetch join은 함께 쓰지 않음).
    public Page<OrderSummaryResponse> myOrders(Long memberId, Pageable pageable) {
        return ordersRepository.findByMemberId(memberId, pageable).map(OrderSummaryResponse::from);
    }

    // 환불 버튼 표시용: 결제 완료, 기간 이내, 환불 전, 읽지 않은 항목. 실제 환불할 때 서버가 다시 확인합니다.
    public OrderDetailResponse myOrder(Long memberId, String orderNo) {
        var order = orderService.findOwned(memberId, orderNo);
        boolean withinPeriod = order.getStatus() == OrderStatus.PAID && order.getPaidAt() != null
                && !order.getPaidAt().plusDays(policy.refundDays()).isBefore(LocalDateTime.now(clock));
        Set<Long> readItemIds = bookshelfRepository
                .findByOrderItemIdIn(order.getItems().stream().map(OrderItem::getId).toList()).stream()
                .filter(Bookshelf::hasBeenRead).map(Bookshelf::getOrderItemId).collect(Collectors.toSet());
        Set<Long> refundedItemIds = order.getItems().stream().filter(OrderItem::isRefunded).map(OrderItem::getId)
                .collect(Collectors.toSet());
        return OrderDetailResponse.of(order, paymentRepository.findByOrderId(order.getId()).orElse(null),
                itemId -> withinPeriod && !readItemIds.contains(itemId) && !refundedItemIds.contains(itemId));
    }
}
