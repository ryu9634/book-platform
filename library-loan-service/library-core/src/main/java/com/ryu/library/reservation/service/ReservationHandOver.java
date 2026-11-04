package com.ryu.library.reservation.service;

import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.global.policy.LibraryPolicy;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 서가로 돌아온 권(반납, 예약 취소, 수령 기한 만료)을 다음 대기자에게 넘깁니다. 대기자가 없으면 대출 가능 상태로 둡니다.
// 호출하는 쪽의 트랜잭션 안에서 실행됩니다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationHandOver {

    private final ReservationRepository reservationRepository;
    private final LibraryPolicy policy;
    private final Clock clock;

    public void handOver(BookCopy copy) {
        reservationRepository
                .findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(copy.getBook().getId(), ReservationStatus.WAITING)
                .ifPresent(next -> hold(copy, next));
    }

    private void hold(BookCopy copy, Reservation next) {
        copy.holdForReservation();
        next.notifyAvailable(LocalDateTime.now(clock), policy.reservationHoldDays());
        log.info("reserved copy held. reservationId={}, memberId={}, bookCopyId={}",
                next.getId(), next.getMemberId(), copy.getId());
    }
}
