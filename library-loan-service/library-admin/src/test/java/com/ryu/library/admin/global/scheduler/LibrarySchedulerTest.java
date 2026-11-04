package com.ryu.library.admin.global.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ryu.library.loan.service.LoanService;
import com.ryu.library.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibrarySchedulerTest {

    private final LoanService loanService = mock(LoanService.class);
    private final ReservationService reservationService = mock(ReservationService.class);
    private final LibraryScheduler scheduler = new LibraryScheduler(loanService, reservationService);

    @Test
    @DisplayName("정기 작업은 core 서비스의 연체 전환과 예약 만료를 부른다")
    void 정기작업_서비스호출() {
        scheduler.markOverdueLoans();
        scheduler.expireReservationHolds();

        verify(loanService).markOverdueLoans();
        verify(reservationService).expireOverdueHolds();
    }
}
