package com.ryu.ebook.admin.order.service;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.order.repository.AdminOrderView;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.order.service.RefundService;
import com.ryu.ebook.payment.domain.Payment;
import com.ryu.ebook.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final int DEFAULT_DAYS = 30;

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final RefundService refundService;
    private final Clock clock;

    // 기간을 비우면 최근 30일입니다. to는 그날을 포함합니다.
    public Page<AdminOrderView> search(OrderStatus status, LocalDate from, LocalDate to, String keyword, Pageable pageable) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS) : from;
        var statuses = status == null ? EnumSet.allOf(OrderStatus.class) : EnumSet.of(status);
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return ordersRepository.searchForAdmin(statuses, start.atStartOfDay(), end.plusDays(1).atStartOfDay(), normalized, pageable);
    }

    // 엔티티를 화면에 넘기지 않고 트랜잭션 안에서 DTO로 바꿉니다(open-in-view가 꺼져 있어 주문 항목 지연 로딩이 화면에서 실패함).
    public Detail get(String orderNo) {
        Orders order = ordersRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        var member = memberRepository.findById(order.getMemberId()).orElse(null);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return new Detail(order.getOrderNo(), order.getStatus(), order.getPaidAmount(),
                member == null ? "-" : member.getName() + " (" + member.getLoginId() + ")",
                payment == null ? "-" : payment.getMethod() + " / " + payment.getStatus() + " / "
                        + (payment.getPgTid() == null ? "-" : payment.getPgTid()),
                order.getItems().stream()
                        .map(i -> new Detail.Item(i.getTitleSnapshot(), i.getPriceSnapshot(), i.getStatus().name()))
                        .toList());
    }

    // 강제 환불은 트랜잭션 밖에서 PG를 부르므로 이 메서드에는 트랜잭션을 걸지 않습니다.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void refundAll(String orderNo) {
        refundService.refundAllByAdmin(orderNo);
    }

    public record Detail(String orderNo, OrderStatus status, long paidAmount, String memberLabel, String paymentLabel,
            java.util.List<Item> items) {

        public boolean refundable() {
            return status == OrderStatus.PAID;
        }

        public record Item(String title, long price, String status) {
        }
    }
}
