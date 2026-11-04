package com.ryu.library.admin.home.service;

import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public Summary summary() {
        LocalDate today = LocalDate.now(clock);
        return new Summary(
                loanRepository.countByLoanedAtGreaterThanEqual(today.atStartOfDay()),
                loanRepository.countByReturnedAtGreaterThanEqual(today.atStartOfDay()),
                loanRepository.countOverdue(today),
                reservationRepository.countByStatus(ReservationStatus.WAITING));
    }

    public record Summary(long todayLoans, long todayReturns, long overdueLoans, long waitingReservations) {
    }
}
