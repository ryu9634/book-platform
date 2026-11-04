package com.ryu.ebook.admin.sales.service;

import com.ryu.ebook.order.repository.SalesRepository;
import com.ryu.ebook.order.repository.SalesRow;
import com.ryu.ebook.order.repository.SalesTotal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesService {

    private static final int TOP_LIMIT = 10;

    private final SalesRepository salesRepository;
    private final Clock clock;

    public Monthly monthly(YearMonth month) {
        YearMonth target = month == null ? YearMonth.now(clock) : month;
        var from = target.atDay(1).atStartOfDay();
        var to = target.plusMonths(1).atDay(1).atStartOfDay();
        return new Monthly(target, salesRepository.total(from, to), salesRepository.byCategory(from, to),
                salesRepository.topEbooks(from, to, PageRequest.of(0, TOP_LIMIT)));
    }

    public SalesTotal today() {
        LocalDate today = LocalDate.now(clock);
        return salesRepository.total(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    public record Monthly(YearMonth month, SalesTotal total, List<SalesRow> byCategory, List<SalesRow> topEbooks) {
    }
}
