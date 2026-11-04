package com.ryu.library.reservation.repository;

import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId,
            Collection<ReservationStatus> statuses);

    // 반납된 권은 가장 먼저 예약한 대기자에게 갑니다.
    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);

    boolean existsByBookIdAndStatus(Long bookId, ReservationStatus status);

    // 대기 순번: 나보다 먼저 예약한 대기자 수 + 1
    @Query("""
            select count(r) + 1 from Reservation r
            where r.bookId = :bookId and r.status = com.ryu.library.reservation.domain.ReservationStatus.WAITING
              and (r.reservedAt < :reservedAt or (r.reservedAt = :reservedAt and r.id < :id))
            """)
    long waitingPosition(@Param("bookId") Long bookId, @Param("reservedAt") LocalDateTime reservedAt, @Param("id") Long id);

    @Query("""
            select new com.ryu.library.reservation.repository.ReservationView(r.id, b.id, b.title, r.reservedAt, r.status, r.expiresAt)
            from Reservation r join Book b on b.id = r.bookId
            where r.memberId = :memberId and r.status in :statuses
            order by r.reservedAt desc
            """)
    List<ReservationView> findViewsByMember(@Param("memberId") Long memberId,
            @Param("statuses") Collection<ReservationStatus> statuses);

    List<Reservation> findByStatusAndExpiresAtLessThanEqual(ReservationStatus status, LocalDateTime now);
}
