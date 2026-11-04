package com.ryu.ebook.payment.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 주문 1건당 결제 1건(ORDER_ID UNIQUE). 실패하면 새로 만들지 않고 같은 결제를 다시 승인합니다.
// READY → APPROVED → CANCELED(환불), READY/FAILED → FAILED(재시도 가능)
@Getter
@Entity
@Table(name = "PAYMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PAYMENT_SEQ_GENERATOR")
    @SequenceGenerator(name = "PAYMENT_SEQ_GENERATOR", sequenceName = "PAYMENT_SEQ", allocationSize = 50)
    @Column(name = "PAYMENT_ID")
    private Long id;

    // 주문은 다른 애그리거트라 ID로만 참조합니다.
    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String pgTid;

    @Column(length = 500)
    private String failReason;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    private Payment(Long orderId, PaymentMethod method, long amount) {
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    public static Payment ready(Long orderId, PaymentMethod method, long amount) {
        return new Payment(orderId, method, amount);
    }

    public void approve(String pgTid, LocalDateTime now) {
        if (status != PaymentStatus.READY && status != PaymentStatus.FAILED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }
        this.status = PaymentStatus.APPROVED;
        this.pgTid = pgTid;
        this.failReason = null;
        this.approvedAt = now;
    }

    public void fail(String reason) {
        if (status == PaymentStatus.READY || status == PaymentStatus.FAILED) {
            this.status = PaymentStatus.FAILED;
            this.failReason = reason;
        }
    }

    public void cancel(LocalDateTime now) {
        if (status != PaymentStatus.CANCELED) {
            this.status = PaymentStatus.CANCELED;
            this.canceledAt = now;
        }
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }
}
