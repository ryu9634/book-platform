package com.ryu.library.user.loan.dto;

import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.repository.LoanView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record LoanResponse(
        Long loanId,
        Long bookId,
        String title,
        String barcode,
        LocalDateTime loanedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt,
        LoanStatus status,
        int extendCount,
        boolean overdue,
        long overdueDays) {

    public static LoanResponse of(LoanView view, LocalDate today) {
        boolean active = view.status() != LoanStatus.RETURNED;
        boolean overdue = active && (view.status() == LoanStatus.OVERDUE || today.isAfter(view.dueDate()));
        long overdueDays = overdue ? ChronoUnit.DAYS.between(view.dueDate(), today) : 0;
        return new LoanResponse(view.loanId(), view.bookId(), view.title(), view.barcode(), view.loanedAt(),
                view.dueDate(), view.returnedAt(), view.status(), view.extendCount(), overdue, overdueDays);
    }

    public String statusLabel() {
        if (status == LoanStatus.RETURNED) {
            return "반납 완료";
        }
        return overdue ? "연체 " + overdueDays + "일" : "대출 중";
    }
}
