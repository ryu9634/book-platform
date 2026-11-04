package com.ryu.library.reservation.service;

import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    public static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.WAITING, ReservationStatus.NOTIFIED);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final ReservationHandOver handOver;
    private final Clock clock;

    // 예약 조건: 대출 가능한 권이 없음, 이 도서를 빌리고 있지 않음, 이미 예약하지 않음.
    // 동시에 같은 예약을 두 번 넣으면 활성 예약 유니크 인덱스(UK_RESERVATION_ACTIVE)가 막습니다.
    @Transactional
    public Long reserve(Long memberId, Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isWithdrawn()) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        List<BookCopy> copies = bookCopyRepository.findByBookIdOrderByIdAsc(bookId);
        if (copies.stream().noneMatch(copy -> copy.getStatus() == BookCopyStatus.ON_LOAN
                || copy.getStatus() == BookCopyStatus.RESERVED || copy.isAvailable())) {
            throw new BusinessException(ErrorCode.NO_COPY_TO_RESERVE);
        }
        if (copies.stream().anyMatch(BookCopy::isAvailable)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_ALLOWED_AVAILABLE);
        }
        if (loanRepository.existsActiveLoanOfBook(memberId, bookId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_ALLOWED_LOANED);
        }
        if (reservationRepository.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId, ACTIVE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION);
        }
        Reservation reservation = reservationRepository.save(Reservation.create(memberId, bookId, LocalDateTime.now(clock)));
        log.info("book reserved. reservationId={}, memberId={}, bookId={}", reservation.getId(), memberId, bookId);
        return reservation.getId();
    }

    // 수령 가능 상태에서 취소하면 보관하던 권을 다음 대기자에게 넘깁니다.
    @Transactional
    public void cancel(Long memberId, Long reservationId) {
        Reservation reservation = findOwned(memberId, reservationId);
        boolean wasHolding = reservation.getStatus() == ReservationStatus.NOTIFIED;
        reservation.cancel();
        if (wasHolding) {
            releaseHeldCopy(reservation.getBookId());
        }
        log.info("reservation canceled. reservationId={}, memberId={}", reservationId, memberId);
    }

    // 수령 기한이 지난 예약을 만료시키고, 보관하던 권을 다음 대기자에게 넘깁니다. 스케줄러가 부릅니다.
    @Transactional
    public int expireOverdueHolds() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtLessThanEqual(ReservationStatus.NOTIFIED, now);
        expired.forEach(reservation -> {
            reservation.expire();
            releaseHeldCopy(reservation.getBookId());
        });
        if (!expired.isEmpty()) {
            log.info("reservation holds expired. count={}", expired.size());
        }
        return expired.size();
    }

    private void releaseHeldCopy(Long bookId) {
        bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(bookId, BookCopyStatus.RESERVED).stream()
                .findFirst()
                .ifPresent(copy -> {
                    copy.releaseReservation();
                    handOver.handOver(copy);
                });
    }

    // 다른 회원의 예약은 "없음"과 똑같이 처리해 존재 여부가 드러나지 않게 합니다.
    private Reservation findOwned(Long memberId, Long reservationId) {
        return reservationRepository.findById(reservationId)
                .filter(reservation -> reservation.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }
}
