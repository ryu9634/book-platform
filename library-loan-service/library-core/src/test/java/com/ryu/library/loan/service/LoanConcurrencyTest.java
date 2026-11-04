package com.ryu.library.loan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.support.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// 실제 Oracle에서 두 회원이 마지막 한 권을 동시에 대출하는 상황을 재현합니다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LoanConcurrencyTest {

    @Autowired
    LoanService loanService;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookCopyRepository bookCopyRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LoanRepository loanRepository;

    @AfterEach
    void cleanUp() {
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("남은 한 권을 두 회원이 동시에 대출하면 한 명만 성공하고 대출 기록도 하나만 생긴다")
    void loanBook_동시대출_한명만성공() throws InterruptedException {
        // given
        Book book = bookRepository.save(BookFixture.book("9780000000061", "마지막 한 권"));
        BookCopy copy = bookCopyRepository.save(BookCopy.create(book, "LIB-CONC-001"));
        Member first = memberRepository.save(MemberFixture.activeMember("conc01", "conc01@example.com"));
        Member second = memberRepository.save(MemberFixture.activeMember("conc02", "conc02@example.com"));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        // when
        for (Member member : List.of(first, second)) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    loanService.loanBook(member.getId(), book.getId());
                    success.incrementAndGet();
                } catch (Exception e) {
                    failures.add(e.getClass().getSimpleName());
                }
                return null;
            });
        }
        ready.await();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // then: 늦은 쪽은 낙관적 락 충돌(동시에 읽은 경우) 또는 가용 권 없음(먼저 끝난 뒤 읽은 경우)으로 실패
        assertThat(success.get()).isEqualTo(1);
        assertThat(failures).hasSize(1)
                .allMatch(name -> name.equals("ObjectOptimisticLockingFailureException") || name.equals("BusinessException"));
        assertThat(loanRepository.count()).isEqualTo(1);
        assertThat(bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
    }
}
