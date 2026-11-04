package com.ryu.library.loan.service;

import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.global.policy.LibraryPolicy;
import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import com.ryu.library.reservation.service.ReservationHandOver;
import com.ryu.library.reservation.service.ReservationService;
import java.util.Optional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 대출 처리. 회원 앱(회원 직접 대출)과 관리자 앱(데스크 처리)이 함께 쓰므로 core에 둡니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanService {

    static final Set<LoanStatus> ACTIVE = EnumSet.of(LoanStatus.ON_LOAN, LoanStatus.OVERDUE);

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationHandOver reservationHandOver;
    private final LibraryPolicy policy;
    private final Clock clock;

    // 도서 단위 대출: 대출 가능한 권 중 하나를 고릅니다.
    // 두 회원이 같은 권을 동시에 고르면 BookCopy의 @Version 때문에 늦은 쪽 커밋이 실패하고 409로 안내됩니다.
    @Transactional
    public Long loanBook(Long memberId, Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        validateBorrower(memberId, bookId);
        Optional<Reservation> reservation = reservationRepository
                .findFirstByMemberIdAndBookIdAndStatusIn(memberId, bookId, ReservationService.ACTIVE);

        // 수령 가능한 예약이 있으면 나를 위해 보관 중인 권을 대출합니다.
        if (reservation.filter(r -> r.getStatus() == ReservationStatus.NOTIFIED).isPresent()) {
            BookCopy held = bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(bookId, BookCopyStatus.RESERVED).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_COPY));
            held.lendToReserver();
            reservation.get().complete();
            return saveLoan(memberId, held);
        }

        BookCopy copy = bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(bookId, BookCopyStatus.AVAILABLE).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_COPY));
        // 대기 중이던 회원이 서가에 있는 권으로 바로 빌리면 그 대기 예약은 끝난 것으로 봅니다.
        reservation.ifPresent(Reservation::complete);
        return lend(memberId, copy);
    }

    // 권 단위 대출: 회원이나 데스크가 특정 권(바코드)을 지정합니다.
    @Transactional
    public Long loanCopy(Long memberId, Long bookCopyId) {
        BookCopy copy = bookCopyRepository.findById(bookCopyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));
        validateBorrower(memberId, copy.getBook().getId());
        return lend(memberId, copy);
    }

    private Long lend(Long memberId, BookCopy copy) {
        copy.lend();
        return saveLoan(memberId, copy);
    }

    private Long saveLoan(Long memberId, BookCopy copy) {
        Loan loan = loanRepository.save(Loan.start(memberId, copy.getId(), LocalDateTime.now(clock), policy.loanDays()));
        log.info("book loaned. loanId={}, memberId={}, bookCopyId={}", loan.getId(), memberId, copy.getId());
        return loan.getId();
    }

    // 회원 본인 반납. 다른 회원의 대출은 "없음"과 똑같이 처리합니다.
    @Transactional
    public void returnByMember(Long memberId, Long loanId) {
        returnLoan(findOwned(memberId, loanId));
    }

    // 관리자 데스크 반납
    @Transactional
    public void returnByDesk(Long loanId) {
        returnLoan(loanRepository.findById(loanId).orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND)));
    }

    // 반납: 대출 종료 → 연체면 연체 일수만큼 정지 → 권을 서가로 → 대기자가 있으면 그 사람 몫으로 보관. 한 트랜잭션.
    private void returnLoan(Loan loan) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        long overdueDays = loan.overdueDays(today);
        loan.returnBook(now);

        if (overdueDays > 0) {
            memberRepository.findById(loan.getMemberId())
                    .ifPresent(member -> member.suspendUntil(today.plusDays(overdueDays)));
        }

        BookCopy copy = bookCopyRepository.findById(loan.getBookCopyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));
        copy.returnToShelf();
        reservationHandOver.handOver(copy);
        log.info("book returned. loanId={}, memberId={}, overdueDays={}", loan.getId(), loan.getMemberId(), overdueDays);
    }

    // 연장: 1회, 7일. 연체 중이거나 이 도서를 기다리는 예약자가 있으면 연장할 수 없습니다.
    @Transactional
    public void extend(Long memberId, Long loanId) {
        Loan loan = findOwned(memberId, loanId);
        BookCopy copy = bookCopyRepository.findById(loan.getBookCopyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));
        if (reservationRepository.existsByBookIdAndStatus(copy.getBook().getId(), ReservationStatus.WAITING)) {
            throw new BusinessException(ErrorCode.EXTEND_NOT_ALLOWED_RESERVED);
        }
        loan.extend(LocalDate.now(clock), policy.extendDays(), policy.maxExtendCount());
        log.info("loan extended. loanId={}, dueDate={}", loanId, loan.getDueDate());
    }

    // 반납 예정일이 지난 대출을 연체로 바꿉니다. 스케줄러가 부릅니다.
    @Transactional
    public int markOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        var loans = loanRepository.findByStatusAndDueDateLessThan(LoanStatus.ON_LOAN, today);
        loans.forEach(loan -> loan.markOverdue(today));
        if (!loans.isEmpty()) {
            log.info("loans marked overdue. count={}", loans.size());
        }
        return loans.size();
    }

    private Loan findOwned(Long memberId, Long loanId) {
        return loanRepository.findById(loanId)
                .filter(loan -> loan.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
    }

    // 대출 조건: 탈퇴/정지 아님, 연체 없음, 최대 권수 미만, 같은 도서를 이미 빌리지 않음
    private void validateBorrower(Long memberId, Long bookId) {
        LocalDate today = LocalDate.now(clock);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.validateCanBorrow(today);
        if (loanRepository.existsOverdue(memberId, today)) {
            throw new BusinessException(ErrorCode.HAS_OVERDUE_LOAN);
        }
        if (loanRepository.countByMemberIdAndStatusIn(memberId, ACTIVE) >= policy.maxLoanCount()) {
            throw new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED, policy.maxLoanCount());
        }
        if (loanRepository.existsActiveLoanOfBook(memberId, bookId)) {
            throw new BusinessException(ErrorCode.ALREADY_LOANED_BOOK);
        }
    }
}
