package com.ryu.ebook.admin.home.service;

import com.ryu.ebook.admin.sales.service.SalesService;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.repository.OrdersRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final OrdersRepository ordersRepository;
    private final SalesService salesService;
    private final Clock clock;

    public Summary summary() {
        return new Summary(
                ordersRepository.countByOrderedAtGreaterThanEqual(LocalDate.now(clock).atStartOfDay()),
                salesService.today().amount(),
                ordersRepository.countByStatus(OrderStatus.PENDING));
    }

    public record Summary(long todayOrders, long todaySales, long pendingOrders) {
    }
}
