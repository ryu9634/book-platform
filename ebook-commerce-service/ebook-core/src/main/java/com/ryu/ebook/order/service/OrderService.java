package com.ryu.ebook.order.service;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.cart.repository.CartRepository;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.repository.PaymentRepository;
import com.ryu.ebook.payment.domain.Payment;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final EbookRepository ebookRepository;
    private final BookshelfRepository bookshelfRepository;
    private final CartRepository cartRepository;
    private final Clock clock;

    // 주문 전 확인용: 판매 중이고 아직 보유하지 않은 전자책만 주문할 수 있습니다.
    public List<Ebook> orderableEbooks(Long memberId, Collection<Long> ebookIds) {
        List<Long> ids = List.copyOf(new LinkedHashSet<>(ebookIds));
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER);
        }
        Map<Long, Ebook> ebooks = ebookRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ebook::getId, Function.identity()));
        if (ebooks.size() != ids.size()) {
            throw new BusinessException(ErrorCode.EBOOK_NOT_FOUND);
        }
        ebooks.values().stream().filter(e -> !e.isOnSale()).findFirst().ifPresent(e -> {
            throw new BusinessException(ErrorCode.EBOOK_NOT_ON_SALE);
        });
        if (!bookshelfRepository.findByMemberIdAndEbookIdInAndActiveTrue(memberId, ids).isEmpty()) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED);
        }
        return ids.stream().map(ebooks::get).toList();
    }

    // 주문 생성: 주문(PENDING) + 항목(가격 스냅샷) + 결제(READY)를 한 트랜잭션으로 만들고, 장바구니에서 해당 항목을 뺍니다.
    @Transactional
    public String create(Long memberId, Collection<Long> ebookIds, PaymentMethod method) {
        List<Ebook> ebooks = orderableEbooks(memberId, ebookIds);
        LocalDateTime now = LocalDateTime.now(clock);
        String orderNo = now.format(ORDER_DATE) + "-" + "%06d".formatted(ordersRepository.nextOrderNoSequence());
        Orders order = ordersRepository.save(Orders.create(orderNo, memberId, now,
                ebooks.stream().map(e -> new Orders.Line(e.getId(), e.getTitle(), e.getSalePrice())).toList()));
        paymentRepository.save(Payment.ready(order.getId(), method == null ? PaymentMethod.MOCK : method,
                order.getPaidAmount()));
        cartRepository.findByMemberId(memberId).ifPresent(cart -> cart.removeEbooks(ebooks.stream().map(Ebook::getId).toList()));
        log.info("order created. orderNo={}, memberId={}, amount={}", orderNo, memberId, order.getPaidAmount());
        return orderNo;
    }

    @Transactional
    public void cancel(Long memberId, String orderNo) {
        Orders order = findOwned(memberId, orderNo);
        cancelPending(order);
    }

    // 결제하지 않고 일정 시간이 지난 주문을 취소합니다. 스케줄러가 부릅니다.
    @Transactional
    public int cancelExpiredPending(Duration timeout) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(timeout);
        List<Orders> expired = ordersRepository.findByStatusAndOrderedAtBefore(OrderStatus.PENDING, cutoff);
        expired.forEach(this::cancelPending);
        if (!expired.isEmpty()) {
            log.info("expired pending orders canceled. count={}", expired.size());
        }
        return expired.size();
    }

    public Orders findOwned(Long memberId, String orderNo) {
        return ordersRepository.findByOrderNo(orderNo)
                .filter(order -> order.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void cancelPending(Orders order) {
        LocalDateTime now = LocalDateTime.now(clock);
        order.cancel(now);
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> payment.cancel(now));
    }
}
