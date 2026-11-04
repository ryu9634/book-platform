package com.ryu.library.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoanTest {

    private static final LocalDateTime LOANED = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Test
    @DisplayName("대출하면 대출일에 대출 기간을 더한 날이 반납 예정일이다")
    void start_반납예정일() {
        assertThat(Loan.start(1L, 10L, LOANED, 14).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    @DisplayName("반납 예정일 다음 날부터 연체이고, 연체 일수는 예정일과의 차이다")
    void isOverdue_예정일다음날부터() {
        Loan loan = Loan.start(1L, 10L, LOANED, 14);
        assertThat(loan.isOverdue(LocalDate.of(2026, 11, 15))).isFalse();
        assertThat(loan.isOverdue(LocalDate.of(2026, 11, 16))).isTrue();
        assertThat(loan.overdueDays(LocalDate.of(2026, 11, 18))).isEqualTo(3);
    }
}
