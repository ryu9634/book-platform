package com.ryu.library.loan.repository;

import com.ryu.library.loan.domain.LoanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 관리자 대출 현황용 조회 결과. 회원, 소장 자료, 도서를 한 번의 조인으로 가져옵니다.
public record AdminLoanView(
        Long loanId,
        Long memberId,
        String loginId,
        String memberName,
        Long bookId,
        String title,
        String barcode,
        LocalDateTime loanedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt,
        LoanStatus status) {

    public boolean isOverdue(LocalDate today) {
        return status == LoanStatus.OVERDUE || (status == LoanStatus.ON_LOAN && today.isAfter(dueDate));
    }

    public long overdueDays(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }
}
