package com.ryu.ebook.order.repository;

import com.ryu.ebook.order.domain.Orders;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// 매출 집계 쿼리. 매출은 결제 시각이 기간 안이고 환불하지 않은(ORDERED) 항목의 주문 당시 가격 합입니다.
public interface SalesRepository extends Repository<Orders, Long> {

    @Query("""
            select new com.ryu.ebook.order.repository.SalesTotal(coalesce(sum(i.priceSnapshot), 0), count(distinct o.id), count(i))
            from Orders o join o.items i
            where o.paidAt >= :from and o.paidAt < :to
              and i.status = com.ryu.ebook.order.domain.OrderItemStatus.ORDERED
            """)
    SalesTotal total(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 분류가 없는 전자책도 빠지지 않도록 분류는 left join 합니다.
    @Query("""
            select new com.ryu.ebook.order.repository.SalesRow(coalesce(c.name, '분류 없음'), sum(i.priceSnapshot), count(i))
            from Orders o join o.items i join Ebook e on e.id = i.ebookId left join Category c on c.id = e.categoryId
            where o.paidAt >= :from and o.paidAt < :to
              and i.status = com.ryu.ebook.order.domain.OrderItemStatus.ORDERED
            group by c.name
            order by sum(i.priceSnapshot) desc
            """)
    List<SalesRow> byCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select new com.ryu.ebook.order.repository.SalesRow(e.title, sum(i.priceSnapshot), count(i))
            from Orders o join o.items i join Ebook e on e.id = i.ebookId
            where o.paidAt >= :from and o.paidAt < :to
              and i.status = com.ryu.ebook.order.domain.OrderItemStatus.ORDERED
            group by e.id, e.title
            order by count(i) desc, sum(i.priceSnapshot) desc
            """)
    List<SalesRow> topEbooks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
