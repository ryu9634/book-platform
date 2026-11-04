package com.ryu.library.user.reservation.dto;

import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationView;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Long bookId,
        String title,
        LocalDateTime reservedAt,
        ReservationStatus status,
        LocalDateTime expiresAt,
        Long waitingPosition) {

    public static ReservationResponse of(ReservationView view, Long waitingPosition) {
        return new ReservationResponse(view.reservationId(), view.bookId(), view.title(), view.reservedAt(),
                view.status(), view.expiresAt(), waitingPosition);
    }

    public String statusLabel() {
        return switch (status) {
            case WAITING -> waitingPosition + "번째 대기 중";
            case NOTIFIED -> "수령 가능";
            case COMPLETED -> "대출 완료";
            case CANCELED -> "취소";
            case EXPIRED -> "기한 만료";
        };
    }
}
