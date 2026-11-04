package com.ryu.library.user.loan.service;

import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import com.ryu.library.reservation.service.ReservationService;
import java.util.Optional;
import com.ryu.library.user.loan.dto.BookActionState;
import com.ryu.library.user.loan.dto.LoanResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public List<LoanResponse> myActiveLoans(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        return loanRepository.findViewsByMember(memberId, EnumSet.of(LoanStatus.ON_LOAN, LoanStatus.OVERDUE)).stream()
                .map(view -> LoanResponse.of(view, today))
                .toList();
    }

    public Page<LoanResponse> myHistory(Long memberId, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        return loanRepository.findHistoryByMember(memberId, pageable).map(view -> LoanResponse.of(view, today));
    }

    // 도서 상세 버튼: 정지 → 이미 대출 중 → 수령 가능한 예약 → 대출 가능 권 → 대기 중인 예약 → 예약 가능 순서로 판단합니다.
    public BookActionState actionState(Long memberId, Long bookId) {
        LocalDate today = LocalDate.now(clock);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isSuspendedOn(today)) {
            return BookActionState.suspended(member.getSuspendedUntil());
        }
        if (loanRepository.existsActiveLoanOfBook(memberId, bookId)) {
            return BookActionState.of(BookActionState.Type.ALREADY_LOANED);
        }
        Optional<Reservation> reservation = reservationRepository
                .findFirstByMemberIdAndBookIdAndStatusIn(memberId, bookId, ReservationService.ACTIVE);
        if (reservation.filter(r -> r.getStatus() == ReservationStatus.NOTIFIED).isPresent()) {
            return BookActionState.of(BookActionState.Type.RESERVED_READY);
        }
        boolean available = !bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(bookId, BookCopyStatus.AVAILABLE).isEmpty();
        if (available) {
            return BookActionState.of(BookActionState.Type.CAN_LOAN);
        }
        return reservation
                .map(r -> BookActionState.waiting(reservationRepository.waitingPosition(bookId, r.getReservedAt(), r.getId())))
                .orElse(BookActionState.of(BookActionState.Type.NO_AVAILABLE_COPY));
    }
}
