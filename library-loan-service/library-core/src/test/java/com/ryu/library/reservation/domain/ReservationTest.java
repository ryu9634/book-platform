package com.ryu.library.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Test
    @DisplayName("수령 가능 알림을 받으면 보관 기한이 생기고, 기한이 되면 만료 대상이다")
    void notifyAvailable_보관기한() {
        // given
        Reservation reservation = Reservation.create(1L, 7L, NOW);

        // when
        reservation.notifyAvailable(NOW, 3);

        // then
        assertThat(reservation.getExpiresAt()).isEqualTo(NOW.plusDays(3));
        assertThat(reservation.isExpired(NOW.plusDays(3).minusSeconds(1))).isFalse();
        assertThat(reservation.isExpired(NOW.plusDays(3))).isTrue();
    }

    @Test
    @DisplayName("끝난 예약은 다시 취소할 수 없다")
    void cancel_끝난예약_R005() {
        // given
        Reservation reservation = Reservation.create(1L, 7L, NOW);
        reservation.cancel();

        // when, then
        assertThatThrownBy(reservation::cancel).extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_NOT_CANCELABLE);
    }
}
