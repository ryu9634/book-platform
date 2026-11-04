package com.ryu.library.loan.repository;

import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.domain.LoanStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    List<Loan> findByStatusAndDueDateLessThan(LoanStatus status, LocalDate date);

    // 스케줄러가 OVERDUE로 바꾸기 전에도 반납 예정일이 지난 대출은 연체로 봅니다.
    @Query("""
            select count(l) > 0 from Loan l
            where l.memberId = :memberId
              and (l.status = com.ryu.library.loan.domain.LoanStatus.OVERDUE
                   or (l.status = com.ryu.library.loan.domain.LoanStatus.ON_LOAN and l.dueDate < :today))
            """)
    boolean existsOverdue(@Param("memberId") Long memberId, @Param("today") LocalDate today);

    @Query("""
            select count(l) > 0 from Loan l join BookCopy c on c.id = l.bookCopyId
            where l.memberId = :memberId and c.book.id = :bookId
              and l.status in (com.ryu.library.loan.domain.LoanStatus.ON_LOAN, com.ryu.library.loan.domain.LoanStatus.OVERDUE)
            """)
    boolean existsActiveLoanOfBook(@Param("memberId") Long memberId, @Param("bookId") Long bookId);

    @Query("""
            select new com.ryu.library.loan.repository.LoanView(l.id, l.memberId, b.id, b.title, c.barcode,
                   l.loanedAt, l.dueDate, l.returnedAt, l.status, l.extendCount)
            from Loan l join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.memberId = :memberId and l.status in :statuses
            order by l.dueDate asc
            """)
    List<LoanView> findViewsByMember(@Param("memberId") Long memberId, @Param("statuses") Collection<LoanStatus> statuses);

    @Query(value = """
            select new com.ryu.library.loan.repository.LoanView(l.id, l.memberId, b.id, b.title, c.barcode,
                   l.loanedAt, l.dueDate, l.returnedAt, l.status, l.extendCount)
            from Loan l join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.memberId = :memberId
            """,
            countQuery = "select count(l) from Loan l where l.memberId = :memberId")
    Page<LoanView> findHistoryByMember(@Param("memberId") Long memberId, Pageable pageable);
}
