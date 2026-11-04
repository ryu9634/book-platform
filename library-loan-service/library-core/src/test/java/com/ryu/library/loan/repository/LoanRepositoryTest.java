package com.ryu.library.loan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.config.ClockConfig;
import com.ryu.library.global.config.JpaConfig;
import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class LoanRepositoryTest {

    private static final LocalDateTime LOANED = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Autowired
    LoanRepository loanRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookCopyRepository bookCopyRepository;

    @Test
    @DisplayName("내 대출 목록은 도서 제목과 바코드를 조인해 한 번에 가져오고, 같은 도서 대출 여부와 연체 여부를 판단한다")
    void 조회쿼리() {
        // given
        Book book = bookRepository.save(BookFixture.book("9780000000051", "이펙티브 자바"));
        BookCopy copy = bookCopyRepository.save(BookCopy.create(book, "LIB-T-001"));
        loanRepository.saveAndFlush(Loan.start(1L, copy.getId(), LOANED, 14));

        // when
        List<LoanView> views = loanRepository.findViewsByMember(1L, EnumSet.of(LoanStatus.ON_LOAN, LoanStatus.OVERDUE));

        // then
        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.title()).isEqualTo("이펙티브 자바");
            assertThat(view.barcode()).isEqualTo("LIB-T-001");
        });
        assertThat(loanRepository.existsActiveLoanOfBook(1L, book.getId())).isTrue();
        assertThat(loanRepository.existsActiveLoanOfBook(2L, book.getId())).isFalse();
        assertThat(loanRepository.existsOverdue(1L, LocalDate.of(2026, 11, 15))).isFalse();
        assertThat(loanRepository.existsOverdue(1L, LocalDate.of(2026, 11, 16))).isTrue();
        assertThat(loanRepository.findHistoryByMember(1L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "loanedAt")))
                .getTotalElements()).isEqualTo(1);
    }

    @Autowired
    com.ryu.library.member.repository.MemberRepository memberRepository;

    @Test
    @DisplayName("관리자 대출 현황은 회원·도서를 조인해 검색어와 상태로 거르고, 연체와 회원별 대출 수를 한 번에 센다")
    void 관리자조회쿼리() {
        // given
        var member = memberRepository.save(com.ryu.library.member.MemberFixture.activeMember("adminq01", "adminq01@example.com"));
        Book book = bookRepository.save(BookFixture.book("9780000000052", "토비의 스프링"));
        BookCopy copy = bookCopyRepository.save(BookCopy.create(book, "LIB-ADM-001"));
        loanRepository.saveAndFlush(Loan.start(member.getId(), copy.getId(), LOANED, 14));
        var active = EnumSet.of(LoanStatus.ON_LOAN, LoanStatus.OVERDUE);

        // when, then
        assertThat(loanRepository.searchForAdmin(active, "토비", PageRequest.of(0, 10)).getContent())
                .singleElement().satisfies(view -> assertThat(view.loginId()).isEqualTo("adminq01"));
        assertThat(loanRepository.searchForAdmin(active, "LIB-ADM-001", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(loanRepository.searchForAdmin(EnumSet.of(LoanStatus.RETURNED), null, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(loanRepository.findOverdueForAdmin(LocalDate.of(2026, 11, 16))).hasSize(1);
        assertThat(loanRepository.countOverdue(LocalDate.of(2026, 11, 16))).isEqualTo(1);
        assertThat(loanRepository.countActiveByMemberIds(List.of(member.getId())))
                .singleElement().satisfies(row -> assertThat(row.getCount()).isEqualTo(1));
    }
}
