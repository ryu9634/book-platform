package com.ryu.library.reservation.repository;

import com.ryu.library.reservation.domain.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationView(
        Long reservationId,
        Long bookId,
        String title,
        LocalDateTime reservedAt,
        ReservationStatus status,
        LocalDateTime expiresAt) {
}
