package com.ryu.library.admin.global.scheduler;

import com.ryu.library.loan.service.LoanService;
import com.ryu.library.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryScheduler {

    private final LoanService loanService;
    private final ReservationService reservationService;

    // 매일 00:10 반납 예정일이 지난 대출을 연체로 바꿉니다. 대출 서비스는 이 작업 전에도 반납 예정일로 연체를 판단합니다.
    @Scheduled(cron = "${library.schedule.overdue-cron}", zone = "Asia/Seoul")
    public void markOverdueLoans() {
        int count = loanService.markOverdueLoans();
        log.info("scheduled overdue marking done. count={}", count);
    }

    // 수령 기한이 지난 예약을 만료시키고, 보관하던 권을 다음 대기자에게 넘깁니다.
    @Scheduled(cron = "${library.schedule.reservation-expire-cron}", zone = "Asia/Seoul")
    public void expireReservationHolds() {
        int count = reservationService.expireOverdueHolds();
        if (count > 0) {
            log.info("scheduled reservation expiry done. count={}", count);
        }
    }
}
