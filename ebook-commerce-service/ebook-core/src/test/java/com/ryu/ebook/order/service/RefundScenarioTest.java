package com.ryu.ebook.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.bookshelf.service.ReadingService;
import com.ryu.ebook.cart.domain.Cart;
import com.ryu.ebook.cart.repository.CartRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.ebook.storage.EbookFileStorage;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.order.domain.OrderStatus;
import com.ryu.ebook.order.domain.Orders;
import com.ryu.ebook.order.repository.OrdersRepository;
import com.ryu.ebook.payment.domain.PaymentMethod;
import com.ryu.ebook.payment.domain.PaymentStatus;
import com.ryu.ebook.payment.repository.PaymentRepository;
import com.ryu.ebook.payment.service.PaymentService;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

// 결제 → 읽기 → 환불 제한 → 부분 환불 → 전체 환불 → 재구매까지 실제 Oracle에서 확인합니다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RefundScenarioTest {

    @Autowired OrderService orderService;
    @Autowired PaymentService paymentService;
    @Autowired RefundService refundService;
    @Autowired ReadingService readingService;
    @Autowired EbookFileStorage fileStorage;
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
        for (String table : List.of("BOOKSHELF", "PAYMENT", "ORDER_ITEM", "ORDERS", "CART_ITEM", "CART", "EBOOK", "MEMBER")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    private Ebook ebookWithFile(String isbn, String title) {
        Ebook ebook = ebookRepository.save(EbookFixture.ebook(isbn, title, 10000, 9000));
        byte[] body = "본문 내용입니다.".repeat(300).getBytes(StandardCharsets.UTF_8);
        var stored = fileStorage.store("본문.txt", body.length, new ByteArrayInputStream(body));
        transactionTemplate.executeWithoutResult(s -> ebookRepository.findById(ebook.getId()).orElseThrow()
                .attachFile(stored.fileKey(), stored.size(), stored.pageCount()));
        return ebook;
    }

    private Long itemIdOf(String orderNo, Long ebookId) {
        return transactionTemplate.execute(s -> ordersRepository.findByOrderNo(orderNo).orElseThrow().getItems().stream()
                .filter(i -> i.getEbookId().equals(ebookId)).findFirst().orElseThrow().getId());
    }

    @Test
    @DisplayName("읽은 항목은 환불할 수 없고, 읽지 않은 항목은 부분 환불되며, 모두 환불하면 주문과 결제가 끝나고, 다시 살 수 있다")
    void 결제_읽기_환불_재구매() {
        // given: 두 권을 사서 결제
        Ebook a = ebookWithFile("9780000000201", "읽을 책");
        Ebook b = ebookWithFile("9780000000202", "안 읽을 책");
        Member member = memberRepository.save(MemberFixture.activeMember("refund01", "refund01@example.com"));
        cartRepository.save(Cart.create(member.getId()));
        String orderNo = orderService.create(member.getId(), List.of(a.getId(), b.getId()), PaymentMethod.MOCK);
        paymentService.approve(member.getId(), orderNo, false);
        Long shelfA = bookshelfRepository.findByMemberIdAndEbookId(member.getId(), a.getId()).orElseThrow().getId();

        // when: A를 2쪽까지 읽음 → A 환불 거부
        ReadingService.ReadingPage page = readingService.read(member.getId(), shelfA, 2);
        assertThat(page.page()).isEqualTo(2);
        assertThat(readingService.read(member.getId(), shelfA, 0).page()).isEqualTo(2);
        assertThatThrownBy(() -> refundService.refundByMember(member.getId(), orderNo, itemIdOf(orderNo, a.getId())))
                .hasMessageContaining("읽은 전자책은 환불할 수 없습니다");

        // B 부분 환불 → 주문은 결제 완료 유지, B는 서재에서 빠지고 판매 수 0
        refundService.refundByMember(member.getId(), orderNo, itemIdOf(orderNo, b.getId()));
        assertThat(ordersRepository.findByOrderNo(orderNo).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(member.getId(), b.getId())).isFalse();
        assertThat(ebookRepository.findById(b.getId()).orElseThrow().getSalesCount()).isZero();

        // 관리자 강제 환불(남은 A) → 주문 환불 완료, 결제 취소
        refundService.refundAllByAdmin(orderNo);
        Orders order = ordersRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(paymentRepository.findByOrderId(order.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELED);

        // 환불한 B는 다시 살 수 있고, 같은 서재 행이 다시 활성화된다
        String again = orderService.create(member.getId(), List.of(b.getId()), PaymentMethod.MOCK);
        paymentService.approve(member.getId(), again, false);
        assertThat(bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(member.getId(), b.getId())).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM BOOKSHELF WHERE MEMBER_ID = ? AND EBOOK_ID = ?",
                Long.class, member.getId(), b.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 후 7일이 지나면 환불할 수 없고, 남의 서재 책은 읽을 수 없다")
    void 환불기간_남의서재() {
        Ebook a = ebookWithFile("9780000000203", "책");
        Member member = memberRepository.save(MemberFixture.activeMember("refund02", "refund02@example.com"));
        Member other = memberRepository.save(MemberFixture.activeMember("refund03", "refund03@example.com"));
        cartRepository.save(Cart.create(member.getId()));
        String orderNo = orderService.create(member.getId(), List.of(a.getId()), PaymentMethod.MOCK);
        paymentService.approve(member.getId(), orderNo, false);
        jdbcTemplate.update("UPDATE ORDERS SET PAID_AT = PAID_AT - INTERVAL '8' DAY WHERE ORDER_NO = ?", orderNo);
        Long shelf = bookshelfRepository.findByMemberIdAndEbookId(member.getId(), a.getId()).orElseThrow().getId();

        assertThatThrownBy(() -> refundService.refundByMember(member.getId(), orderNo, itemIdOf(orderNo, a.getId())))
                .hasMessageContaining("7일이 지나");
        assertThatThrownBy(() -> readingService.read(other.getId(), shelf, 1))
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKSHELF_NOT_FOUND);
    }
}
