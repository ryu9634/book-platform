package com.ryu.ebook.user.order.service;

import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.order.service.OrderService;
import com.ryu.ebook.payment.repository.PaymentRepository;
import com.ryu.ebook.user.order.dto.CheckoutResponse;
import com.ryu.ebook.user.order.dto.OrderDetailResponse;
import com.ryu.ebook.user.order.dto.OrderSummaryResponse;
import java.util.Collection;
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

    public CheckoutResponse checkout(Long memberId, Collection<Long> ebookIds) {
        return CheckoutResponse.of(orderService.orderableEbooks(memberId, ebookIds));
    }

    // 주문 목록의 항목(컬렉션)은 fetch join 대신 default_batch_fetch_size로 한 번에 가져옵니다(페이징과 컬렉션 fetch join은 함께 쓰지 않음).
    public Page<OrderSummaryResponse> myOrders(Long memberId, Pageable pageable) {
        return ordersRepository.findByMemberId(memberId, pageable).map(OrderSummaryResponse::from);
    }

    public OrderDetailResponse myOrder(Long memberId, String orderNo) {
        var order = orderService.findOwned(memberId, orderNo);
        return OrderDetailResponse.of(order, paymentRepository.findByOrderId(order.getId()).orElse(null));
    }
}
