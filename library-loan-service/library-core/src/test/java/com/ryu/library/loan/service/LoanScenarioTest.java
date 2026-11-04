package com.ryu.library.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.domain.MemberStatus;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import com.ryu.library.reservation.service.ReservationService;
import com.ryu.library.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// 대출 → 예약 → 연체 반납 → 예약자 보관 → 예약자 대출까지 실제 Oracle에서 한 흐름으로 확인합니다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LoanScenarioTest {

    @Autowired LoanService loanService;
    @Autowired ReservationService reservationService;
    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;

    @AfterEach
    void cleanUp() {
        reservationRepository.deleteAll();
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("연체 반납자는 정지되고, 반납된 권은 예약자 몫으로 보관되어 다른 회원은 빌릴 수 없으며, 예약자가 빌리면 예약이 완료된다")
    void 대출_예약_연체반납_예약자대출() {
        // given
        Book book = bookRepository.save(BookFixture.book("9780000000091", "인기 도서"));
        BookCopy copy = bookCopyRepository.save(BookCopy.create(book, "LIB-SCN-001"));
        Member a = memberRepository.save(MemberFixture.activeMember("scena", "scena@example.com"));
        Member b = memberRepository.save(MemberFixture.activeMember("scenb", "scenb@example.com"));
        Member c = memberRepository.save(MemberFixture.activeMember("scenc", "scenc@example.com"));

        // when: A 대출 → B 예약 → (반납 예정일을 3일 전으로 당겨) A 연체 반납
        Long loanId = loanService.loanBook(a.getId(), book.getId());
        Long reservationId = reservationService.reserve(b.getId(), book.getId());
        jdbcTemplate.update("UPDATE LOAN SET DUE_DATE = ? WHERE LOAN_ID = ?",
                java.sql.Date.valueOf(LocalDate.now(clock).minusDays(3)), loanId);
        loanService.returnByMember(a.getId(), loanId);

        // then
        Member suspended = memberRepository.findById(a.getId()).orElseThrow();
        assertThat(suspended.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(suspended.getSuspendedUntil()).isEqualTo(LocalDate.now(clock).plusDays(3));
        assertThat(bookCopyRepository.findById(copy.getId()).orElseThrow().getStatus()).isEqualTo(BookCopyStatus.RESERVED);
        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus()).isEqualTo(ReservationStatus.NOTIFIED);

        // C는 보관 중인 권을 빌릴 수 없고, B는 빌린다
        assertThatThrownBy(() -> loanService.loanBook(c.getId(), book.getId()))
                .extracting("errorCode").isEqualTo(ErrorCode.NO_AVAILABLE_COPY);
        Long bLoanId = loanService.loanBook(b.getId(), book.getId());
        assertThat(loanRepository.findById(bLoanId).orElseThrow().getStatus()).isEqualTo(LoanStatus.ON_LOAN);
        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(loanRepository.findById(loanId).map(Loan::getStatus)).hasValue(LoanStatus.RETURNED);
    }

    @Test
    @DisplayName("같은 회원의 활성 예약이 동시에 두 번 들어가도 활성 예약 유니크 인덱스가 하나만 남긴다")
    void 활성예약_유니크인덱스() {
        // given
        Book book = bookRepository.save(BookFixture.book("9780000000092", "도서"));
        Member member = memberRepository.save(MemberFixture.activeMember("uniq01", "uniq01@example.com"));
        reservationRepository.saveAndFlush(Reservation.create(member.getId(), book.getId(), java.time.LocalDateTime.now(clock)));

        // when, then: 서비스 검사를 건너뛰고 바로 저장해도 DB가 막는다
        assertThatThrownBy(() -> reservationRepository.saveAndFlush(
                Reservation.create(member.getId(), book.getId(), java.time.LocalDateTime.now(clock))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
