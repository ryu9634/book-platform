package com.ryu.ebook.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.category.repository.CategoryRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class SalesRepositoryTest {

    private static final LocalDateTime NOV = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Autowired SalesRepository salesRepository;
    @Autowired OrdersRepository ordersRepository;
    @Autowired EbookRepository ebookRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired TestEntityManager em;

    @Test
    @DisplayName("월 매출은 결제 시각이 그 달이고 환불하지 않은 항목만 더하며, 분류별·상위 전자책도 집계한다")
    void 월매출_분류별_상위() {
        // given: 분류 있는 책 A(9,000), 분류 없는 책 B(18,000)
        Long categoryId = categoryRepository.findAllByOrderBySortOrderAsc().get(0).getId();
        Ebook a = ebookRepository.save(com.ryu.ebook.ebook.domain.Ebook.builder().title("A").author("x").categoryId(categoryId)
                .price(10000).salePrice(9000).build());
        Ebook b = ebookRepository.save(EbookFixture.ebook("9780000000301", "B", 20000, 18000));
        Orders paid = Orders.create("20261103-000001", 1L, NOV, List.of(new Orders.Line(a.getId(), "A", 9000), new Orders.Line(b.getId(), "B", 18000)));
        paid.pay(NOV);
        ordersRepository.saveAndFlush(paid);
        paid.refundItem(paid.getItems().get(1).getId());
        Orders second = Orders.create("20261103-000002", 2L, NOV, List.of(new Orders.Line(a.getId(), "A", 9000)));
        second.pay(NOV);
        Orders pending = Orders.create("20261103-000003", 3L, NOV, List.of(new Orders.Line(b.getId(), "B", 18000)));
        Orders lastMonth = Orders.create("20261003-000001", 4L, NOV.minusMonths(1), List.of(new Orders.Line(a.getId(), "A", 9000)));
        lastMonth.pay(NOV.minusMonths(1));
        ordersRepository.saveAll(List.of(second, pending, lastMonth));
        em.flush();

        var from = LocalDateTime.of(2026, 11, 1, 0, 0);
        var to = LocalDateTime.of(2026, 12, 1, 0, 0);

        // when, then: 환불한 B, 결제 안 한 주문, 지난달 주문은 빠진다
        SalesTotal total = salesRepository.total(from, to);
        assertThat(total.amount()).isEqualTo(18000);
        assertThat(total.orderCount()).isEqualTo(2);
        assertThat(salesRepository.byCategory(from, to)).singleElement()
                .satisfies(row -> assertThat(row.amount()).isEqualTo(18000));
        assertThat(salesRepository.topEbooks(from, to, PageRequest.of(0, 10))).extracting(SalesRow::name).containsExactly("A");
    }
}
