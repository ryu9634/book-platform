package com.ryu.ebook.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.cart.domain.Cart;
import com.ryu.ebook.cart.repository.CartRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.domain.PaymentStatus;
import com.ryu.ebook.payment.repository.PaymentRepository;
import com.ryu.ebook.payment.service.PaymentService;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

// 장바구니 → 주문 → 결제 실패 → 재결제 승인 → 서재 생성까지 실제 Oracle에서 한 흐름으로 확인합니다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderPaymentScenarioTest {

    @Autowired OrderService orderService;
    @Autowired PaymentService paymentService;
    @Autowired OrdersRepository ordersRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired BookshelfRepository bookshelfRepository;
    @Autowired EbookRepository ebookRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CartRepository cartRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM BOOKSHELF");
        jdbcTemplate.update("DELETE FROM PAYMENT");
        jdbcTemplate.update("DELETE FROM ORDER_ITEM");
        jdbcTemplate.update("DELETE FROM ORDERS");
        jdbcTemplate.update("DELETE FROM CART_ITEM");
        jdbcTemplate.update("DELETE FROM CART");
        jdbcTemplate.update("DELETE FROM EBOOK");
        jdbcTemplate.update("DELETE FROM MEMBER");
    }

    private Member memberWithCart(String loginId, Long... ebookIds) {
        Member member = memberRepository.save(MemberFixture.activeMember(loginId, loginId + "@example.com"));
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = cartRepository.save(Cart.create(member.getId()));
            for (Long id : ebookIds) {
                cart.add(id);
            }
        });
        return member;
    }

    @Test
    @DisplayName("주문하면 장바구니에서 빠지고, 결제 실패 후 다시 승인하면 서재가 생기고 판매 수가 오르며, 같은 주문은 다시 결제할 수 없다")
    void 주문_실패_재결제_서재() {
        // given
        Ebook a = ebookRepository.save(EbookFixture.ebook("9780000000101", "A", 10000, 9000));
        Ebook b = ebookRepository.save(EbookFixture.ebook("9780000000102", "B", 20000, 18000));
        Member member = memberWithCart("order01", a.getId(), b.getId());

        // when: 주문 생성
        String orderNo = orderService.create(member.getId(), List.of(a.getId(), b.getId()), PaymentMethod.MOCK);

        // then
        assertThat(orderNo).matches("\\d{8}-\\d{6}");
        // 장바구니 항목은 지연 로딩이라 트랜잭션 밖에서는 SQL로 직접 셉니다(open-in-view 꺼짐).
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CART_ITEM ci JOIN CART c ON c.CART_ID = ci.CART_ID WHERE c.MEMBER_ID = ?",
                Long.class, member.getId())).isZero();

        // 결제 실패 → 주문은 결제 대기 유지, 결제는 FAILED
        assertThat(paymentService.approve(member.getId(), orderNo, true).approved()).isFalse();
        var order = ordersRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);

        // 재결제 승인 → 서재 2권, 판매 수 +1, 주문 PAID
        assertThat(paymentService.approve(member.getId(), orderNo, false).approved()).isTrue();
        assertThat(ordersRepository.findByOrderNo(orderNo).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(bookshelfRepository.findByMemberIdAndEbookIdInAndActiveTrue(member.getId(), List.of(a.getId(), b.getId()))).hasSize(2);
        assertThat(ebookRepository.findById(a.getId()).orElseThrow().getSalesCount()).isEqualTo(1);

        // 같은 주문 재결제, 보유한 책 재주문은 거부
        assertThatThrownBy(() -> paymentService.approve(member.getId(), orderNo, false))
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_PENDING);
        assertThatThrownBy(() -> orderService.create(member.getId(), List.of(a.getId()), PaymentMethod.MOCK))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_OWNED);
    }

    @Test
    @DisplayName("같은 주문에 결제 승인이 동시에 두 번 들어와도 한 번만 반영된다")
    void 동시결제_한번만() throws InterruptedException {
        // given
        Ebook a = ebookRepository.save(EbookFixture.ebook("9780000000103", "A", 10000, 9000));
        Member member = memberWithCart("order02");
        String orderNo = orderService.create(member.getId(), List.of(a.getId()), PaymentMethod.MOCK);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                start.await();
                try {
                    results.add(paymentService.approve(member.getId(), orderNo, false).approved());
                } catch (Exception e) {
                    results.add(false);
                }
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(bookshelfRepository.findByMemberIdAndEbookIdInAndActiveTrue(member.getId(), List.of(a.getId()))).hasSize(1);
        assertThat(ebookRepository.findById(a.getId()).orElseThrow().getSalesCount()).isEqualTo(1);
        var order = ordersRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("30분 넘게 결제하지 않은 주문은 취소되고 결제도 취소된다")
    void 미결제주문_자동취소() {
        // given
        Ebook a = ebookRepository.save(EbookFixture.ebook("9780000000104", "A", 10000, 9000));
        Member member = memberWithCart("order03");
        String orderNo = orderService.create(member.getId(), List.of(a.getId()), PaymentMethod.MOCK);
        jdbcTemplate.update("UPDATE ORDERS SET ORDERED_AT = ORDERED_AT - INTERVAL '31' MINUTE WHERE ORDER_NO = ?", orderNo);

        // when
        int canceled = orderService.cancelExpiredPending(Duration.ofMinutes(30));

        // then
        assertThat(canceled).isEqualTo(1);
        var order = ordersRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThatThrownBy(() -> paymentService.approve(member.getId(), orderNo, false))
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_PENDING);
    }
}
