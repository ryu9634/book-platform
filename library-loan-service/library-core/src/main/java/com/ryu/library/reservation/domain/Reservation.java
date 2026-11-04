package com.ryu.library.reservation.domain;

import com.ryu.library.global.common.BaseTimeEntity;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
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

// 예약은 도서 단위입니다. 어느 권이든 먼저 반납되는 권이 예약자에게 갑니다.
// 대기(WAITING) → 수령 가능(NOTIFIED, 기한 있음) → 대출 완료(COMPLETED) / 취소(CANCELED) / 기한 만료(EXPIRED)
@Getter
@Entity
@Table(name = "RESERVATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RESERVATION_SEQ_GENERATOR")
    @SequenceGenerator(name = "RESERVATION_SEQ_GENERATOR", sequenceName = "RESERVATION_SEQ", allocationSize = 50)
    @Column(name = "RESERVATION_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    private LocalDateTime notifiedAt;

    private LocalDateTime expiresAt;

    private Reservation(Long memberId, Long bookId, LocalDateTime reservedAt) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.reservedAt = reservedAt;
        this.status = ReservationStatus.WAITING;
    }

    public static Reservation create(Long memberId, Long bookId, LocalDateTime now) {
        return new Reservation(memberId, bookId, now);
    }

    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.NOTIFIED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void notifyAvailable(LocalDateTime now, int holdDays) {
        require(ReservationStatus.WAITING);
        this.status = ReservationStatus.NOTIFIED;
        this.notifiedAt = now;
        this.expiresAt = now.plusDays(holdDays);
    }

    public void complete() {
        if (!isActive()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
        this.status = ReservationStatus.COMPLETED;
    }

    public void cancel() {
        if (!isActive()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
        this.status = ReservationStatus.CANCELED;
    }

    public boolean isExpired(LocalDateTime now) {
        return status == ReservationStatus.NOTIFIED && expiresAt != null && !now.isBefore(expiresAt);
    }

    public void expire() {
        require(ReservationStatus.NOTIFIED);
        this.status = ReservationStatus.EXPIRED;
    }

    private void require(ReservationStatus expected) {
        if (status != expected) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELABLE);
        }
    }
}
