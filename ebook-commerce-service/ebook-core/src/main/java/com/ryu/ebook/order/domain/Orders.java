package com.ryu.ebook.order.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주문(ORDERS). 클래스 이름을 Order로 하면 JPQL 예약어와 겹쳐서 테이블 이름과 같은 Orders로 둡니다.
// 상태: PENDING(결제 대기) → PAID → (모든 항목 환불 시) REFUNDED, PENDING → CANCELED
@Getter
@Entity
@Table(name = "ORDERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ORDERS_SEQ_GENERATOR")
    @SequenceGenerator(name = "ORDERS_SEQ_GENERATOR", sequenceName = "ORDERS_SEQ", allocationSize = 50)
    @Column(name = "ORDER_ID")
    private Long id;

    // 사람이 읽는 주문번호(yyyyMMdd-000001). 외부(URL, 화면)에는 내부 ID 대신 이 값을 씁니다.
    @Column(nullable = false, length = 20)
    private String orderNo;

    @Column(nullable = false)
    private Long memberId;

    // 쿠폰 적용은 v2에서 합니다. 지금은 항상 비어 있습니다.
    private Long memberCouponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long paidAmount;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Orders(String orderNo, Long memberId, LocalDateTime now) {
        this.orderNo = orderNo;
        this.memberId = memberId;
        this.orderedAt = now;
        this.status = OrderStatus.PENDING;
    }

    public static Orders create(String orderNo, Long memberId, LocalDateTime now, List<Line> lines) {
        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER);
        }
        Orders order = new Orders(orderNo, memberId, now);
        lines.forEach(line -> order.items.add(new OrderItem(order, line.ebookId(), line.title(), line.price())));
        order.totalAmount = lines.stream().mapToLong(Line::price).sum();
        order.discountAmount = 0;
        order.paidAmount = order.totalAmount - order.discountAmount;
        return order;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public void pay(LocalDateTime now) {
        requirePending();
        this.status = OrderStatus.PAID;
        this.paidAt = now;
    }

    public void cancel(LocalDateTime now) {
        requirePending();
        this.status = OrderStatus.CANCELED;
        this.canceledAt = now;
    }

    // 부분 환불: 항목 하나를 환불하고, 모든 항목이 환불되면 주문 전체를 환불 상태로 바꿉니다.
    public OrderItem refundItem(Long orderItemId) {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "결제 완료된 주문만 환불할 수 있습니다.");
        }
        OrderItem item = items.stream().filter(i -> i.getId().equals(orderItemId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        item.refund();
        if (items.stream().allMatch(OrderItem::isRefunded)) {
            this.status = OrderStatus.REFUNDED;
        }
        return item;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }
    }

    public record Line(Long ebookId, String title, long price) {
    }
}
