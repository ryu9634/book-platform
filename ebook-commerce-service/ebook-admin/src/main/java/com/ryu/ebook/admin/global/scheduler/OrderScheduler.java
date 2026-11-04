package com.ryu.ebook.admin.global.scheduler;

import com.ryu.ebook.order.service.OrderService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    @Value("${ebook.schedule.pending-order-timeout}")
    private Duration pendingOrderTimeout;

    // 결제하지 않은 주문을 일정 시간 뒤 취소합니다.
    @Scheduled(cron = "${ebook.schedule.pending-order-cancel-cron}", zone = "Asia/Seoul")
    public void cancelExpiredPendingOrders() {
        orderService.cancelExpiredPending(pendingOrderTimeout);
    }
}
