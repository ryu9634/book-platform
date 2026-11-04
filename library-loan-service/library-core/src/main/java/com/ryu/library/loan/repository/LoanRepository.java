package com.ryu.library.loan.repository;

import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.domain.LoanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    long countByLoanedAtGreaterThanEqual(LocalDateTime from);

    @Query("""
            select new com.ryu.library.loan.repository.PopularBookView(b.id, b.isbn, b.title, b.author, count(l))
            from Loan l join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.loanedAt >= :from
            group by b.id, b.isbn, b.title, b.author
            order by count(l) desc, b.id desc
            """)
    List<PopularBookView> findPopular(@Param("from") LocalDateTime from, Pageable pageable);

    long countByReturnedAtGreaterThanEqual(LocalDateTime from);

    @Query("""
            select count(l) from Loan l
            where l.status = com.ryu.library.loan.domain.LoanStatus.OVERDUE
               or (l.status = com.ryu.library.loan.domain.LoanStatus.ON_LOAN and l.dueDate < :today)
            """)
    long countOverdue(@Param("today") LocalDate today);

    // 관리자 대출 현황: 상태(선택), 검색어(회원 아이디·이름, 도서 제목, 바코드)로 거릅니다.
    @Query(value = """
            select new com.ryu.library.loan.repository.AdminLoanView(l.id, m.id, m.loginId, m.name, b.id, b.title,
                   c.barcode, l.loanedAt, l.dueDate, l.returnedAt, l.status)
            from Loan l join Member m on m.id = l.memberId join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.status in :statuses
              and (:keyword is null or m.loginId like concat('%', :keyword, '%') or m.name like concat('%', :keyword, '%')
                   or b.title like concat('%', :keyword, '%') or c.barcode = :keyword)
            """,
            countQuery = """
            select count(l)
            from Loan l join Member m on m.id = l.memberId join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.status in :statuses
              and (:keyword is null or m.loginId like concat('%', :keyword, '%') or m.name like concat('%', :keyword, '%')
                   or b.title like concat('%', :keyword, '%') or c.barcode = :keyword)
            """)
    Page<AdminLoanView> searchForAdmin(@Param("statuses") Collection<LoanStatus> statuses,
            @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select new com.ryu.library.loan.repository.AdminLoanView(l.id, m.id, m.loginId, m.name, b.id, b.title,
                   c.barcode, l.loanedAt, l.dueDate, l.returnedAt, l.status)
            from Loan l join Member m on m.id = l.memberId join BookCopy c on c.id = l.bookCopyId join c.book b
            where l.status = com.ryu.library.loan.domain.LoanStatus.OVERDUE
               or (l.status = com.ryu.library.loan.domain.LoanStatus.ON_LOAN and l.dueDate < :today)
            order by l.dueDate asc
            """)
    List<AdminLoanView> findOverdueForAdmin(@Param("today") LocalDate today);

    // 회원 목록에서 회원마다 따로 세지 않도록 대출 중 권수를 한 번에 셉니다.
    @Query("""
            select l.memberId as memberId, count(l) as count from Loan l
            where l.memberId in :memberIds
              and l.status in (com.ryu.library.loan.domain.LoanStatus.ON_LOAN, com.ryu.library.loan.domain.LoanStatus.OVERDUE)
            group by l.memberId
            """)
    List<MemberLoanCount> countActiveByMemberIds(@Param("memberIds") Collection<Long> memberIds);

    interface MemberLoanCount {
        Long getMemberId();

        long getCount();
    }

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
