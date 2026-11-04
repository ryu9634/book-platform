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
        BookCopy copy = bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(bookId, BookCopyStatus.AVAILABLE).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AVAILABLE_COPY));
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
        Loan loan = loanRepository.save(Loan.start(memberId, copy.getId(), LocalDateTime.now(clock), policy.loanDays()));
        log.info("book loaned. loanId={}, memberId={}, bookCopyId={}", loan.getId(), memberId, copy.getId());
        return loan.getId();
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
