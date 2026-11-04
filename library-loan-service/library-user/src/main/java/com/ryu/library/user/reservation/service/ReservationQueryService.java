package com.ryu.library.user.reservation.service;

import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import com.ryu.library.reservation.service.ReservationService;
import com.ryu.library.user.reservation.dto.ReservationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;

    // 대기 순번은 대기 중인 예약마다 한 번씩 셉니다. 회원 한 명의 활성 예약은 몇 건뿐이라 따로 묶지 않습니다.
    public List<ReservationResponse> myActiveReservations(Long memberId) {
        return reservationRepository.findViewsByMember(memberId, ReservationService.ACTIVE).stream()
                .map(view -> ReservationResponse.of(view, view.status() == ReservationStatus.WAITING
                        ? reservationRepository.waitingPosition(view.bookId(), view.reservedAt(), view.reservationId())
                        : null))
                .toList();
    }
}
