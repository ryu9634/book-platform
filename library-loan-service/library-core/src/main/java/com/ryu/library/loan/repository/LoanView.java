package com.ryu.library.loan.repository;

import com.ryu.library.loan.domain.LoanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 대출 목록 화면용 조회 결과. Loan은 소장 자료를 ID로만 참조하므로 지연 로딩 대신 JPQL 조인으로 한 번에 가져옵니다.
public record LoanView(
        Long loanId,
        Long memberId,
        Long bookId,
        String title,
        String barcode,
        LocalDateTime loanedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt,
        LoanStatus status,
        int extendCount) {
}
