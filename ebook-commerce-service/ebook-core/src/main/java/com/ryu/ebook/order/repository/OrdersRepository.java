package com.ryu.ebook.order.repository;

import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByOrderNo(String orderNo);

    // 결제 승인 반영 직전에 주문 행을 잠급니다(SELECT ... FOR UPDATE). 같은 주문에 승인이 두 번 반영되지 않게 합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.orderNo = :orderNo")
    Optional<Orders> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    // 주문번호 일련번호. 사람이 읽는 번호라 1씩 증가하는 별도 시퀀스를 씁니다.
    @Query(value = "select ORDER_NO_SEQ.NEXTVAL from dual", nativeQuery = true)
    long nextOrderNoSequence();

    Page<Orders> findByMemberId(Long memberId, Pageable pageable);

    List<Orders> findByStatusAndOrderedAtBefore(OrderStatus status, LocalDateTime cutoff);

    long countByOrderedAtGreaterThanEqual(LocalDateTime from);

    long countByStatus(OrderStatus status);

    // 관리자 주문 조회: 상태 목록, 주문 기간, 검색어(주문번호·회원 아이디·이름)
    @Query(value = """
            select new com.ryu.ebook.order.repository.AdminOrderView(o.orderNo, m.id, m.loginId, m.name, o.status,
                   o.paidAmount, o.orderedAt, o.paidAt)
            from Orders o join Member m on m.id = o.memberId
            where o.status in :statuses and o.orderedAt >= :from and o.orderedAt < :to
              and (:keyword is null or o.orderNo = :keyword or m.loginId like concat('%', :keyword, '%')
                   or m.name like concat('%', :keyword, '%'))
            """,
            countQuery = """
            select count(o) from Orders o join Member m on m.id = o.memberId
            where o.status in :statuses and o.orderedAt >= :from and o.orderedAt < :to
              and (:keyword is null or o.orderNo = :keyword or m.loginId like concat('%', :keyword, '%')
                   or m.name like concat('%', :keyword, '%'))
            """)
    Page<AdminOrderView> searchForAdmin(@Param("statuses") java.util.Collection<OrderStatus> statuses,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("keyword") String keyword,
            Pageable pageable);
}
