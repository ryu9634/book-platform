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
}
