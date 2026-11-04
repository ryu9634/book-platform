package com.ryu.ebook.order.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주문 항목. 나중에 전자책 가격이나 제목이 바뀌어도 주문 내역은 그대로 보이도록 주문 당시 값을 복사해 둡니다.
@Getter
@Entity
@Table(name = "ORDER_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ORDER_ITEM_SEQ_GENERATOR")
    @SequenceGenerator(name = "ORDER_ITEM_SEQ_GENERATOR", sequenceName = "ORDER_ITEM_SEQ", allocationSize = 50)
    @Column(name = "ORDER_ITEM_ID")
    private Long id;

    // 같은 주문 애그리거트 안이라 객체로 참조합니다(FK 없음).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Orders order;

    @Column(nullable = false)
    private Long ebookId;

    @Column(nullable = false, length = 200)
    private String titleSnapshot;

    @Column(nullable = false)
    private long priceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status;

    OrderItem(Orders order, Long ebookId, String title, long price) {
        this.order = order;
        this.ebookId = ebookId;
        this.titleSnapshot = title;
        this.priceSnapshot = price;
        this.status = OrderItemStatus.ORDERED;
    }

    public boolean isRefunded() {
        return status == OrderItemStatus.REFUNDED;
    }

    void refund() {
        if (isRefunded()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "이미 환불한 항목입니다.");
        }
        this.status = OrderItemStatus.REFUNDED;
    }
}
