package com.ryu.library.loan.domain;

import com.ryu.library.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 대출 기록. 회원과 소장 자료는 다른 애그리거트라 ID로만 참조합니다.
@Getter
@Entity
@Table(name = "LOAN")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOAN_SEQ_GENERATOR")
    @SequenceGenerator(name = "LOAN_SEQ_GENERATOR", sequenceName = "LOAN_SEQ", allocationSize = 50)
    @Column(name = "LOAN_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookCopyId;

    @Column(nullable = false)
    private LocalDateTime loanedAt;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(nullable = false)
    private int extendCount;

    private Loan(Long memberId, Long bookCopyId, LocalDateTime loanedAt, LocalDate dueDate) {
        this.memberId = memberId;
        this.bookCopyId = bookCopyId;
        this.loanedAt = loanedAt;
        this.dueDate = dueDate;
        this.status = LoanStatus.ON_LOAN;
    }

    public static Loan start(Long memberId, Long bookCopyId, LocalDateTime now, int loanDays) {
        return new Loan(memberId, bookCopyId, now, now.toLocalDate().plusDays(loanDays));
    }

    public boolean isActive() {
        return status == LoanStatus.ON_LOAN || status == LoanStatus.OVERDUE;
    }

    // 스케줄러가 아직 OVERDUE로 바꾸기 전이어도 반납 예정일이 지났으면 연체로 봅니다.
    public boolean isOverdue(LocalDate today) {
        return status == LoanStatus.OVERDUE || (status == LoanStatus.ON_LOAN && today.isAfter(dueDate));
    }

    public long overdueDays(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
