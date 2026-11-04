package com.ryu.library.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.global.policy.LibraryPolicy;
import com.ryu.library.loan.domain.Loan;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.member.domain.MemberStatus;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import com.ryu.library.reservation.service.ReservationHandOver;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class LoanServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    private static final Long MEMBER_ID = 1L;
    private static final Long BOOK_ID = 7L;

    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final LibraryPolicy policy = new LibraryPolicy(5, 14, 7, 1, 3);
    private LoanService loanService;
    private Member member;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookRepository, bookCopyRepository, memberRepository,
                reservationRepository, new ReservationHandOver(reservationRepository, policy, CLOCK), policy, CLOCK);
        member = MemberFixture.activeMember();
        copy = BookCopy.create(BookFixture.book("9780000000041", "제목"), "LIB-000007-001");
        ReflectionTestUtils.setField(copy, "id", 70L);
        given(bookRepository.existsById(BOOK_ID)).willReturn(true);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(BOOK_ID, BookCopyStatus.AVAILABLE)).willReturn(List.of(copy));
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("조건을 모두 만족하면 대출 가능한 권을 대출 중으로 바꾸고 14일 뒤를 반납 예정일로 기록한다")
    void loanBook_정상_대출() {
        // when
        loanService.loanBook(MEMBER_ID, BOOK_ID);

        // then
        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        assertThat(captor.getValue().getBookCopyId()).isEqualTo(70L);
        assertThat(captor.getValue().getDueDate()).isEqualTo(TODAY.plusDays(14));
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
    }

    @Test
    @DisplayName("이미 5권을 빌렸으면 L001로 거부하고 메시지에 최대 권수를 담는다")
    void loanBook_최대권수_L001() {
        // given
        given(loanRepository.countByMemberIdAndStatusIn(any(), anyCollection())).willReturn(5L);

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, BOOK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5권")
                .extracting("errorCode").isEqualTo(ErrorCode.LOAN_LIMIT_EXCEEDED);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("연체 중인 대출이 있으면 L002로 거부한다")
    void loanBook_연체보유_L002() {
        // given
        given(loanRepository.existsOverdue(MEMBER_ID, TODAY)).willReturn(true);

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, BOOK_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.HAS_OVERDUE_LOAN);
    }

    @Test
    @DisplayName("같은 도서를 이미 빌려 갔으면 L003으로 거부한다")
    void loanBook_같은도서_L003() {
        // given
        given(loanRepository.existsActiveLoanOfBook(MEMBER_ID, BOOK_ID)).willReturn(true);

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, BOOK_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_LOANED_BOOK);
    }

    @Test
    @DisplayName("정지 중인 회원은 M004로 거부한다")
    void loanBook_정지회원_M004() {
        // given
        member.suspendUntil(TODAY.plusDays(2));

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, BOOK_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_SUSPENDED);
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("대출 가능한 권이 없으면 L005로 예약을 안내한다")
    void loanBook_가용권없음_L005() {
        // given
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(BOOK_ID, BookCopyStatus.AVAILABLE)).willReturn(List.of());

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, BOOK_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.NO_AVAILABLE_COPY);
    }

    @Test
    @DisplayName("없는 도서면 B001로 거부한다")
    void loanBook_없는도서_B001() {
        // given
        given(bookRepository.existsById(999L)).willReturn(false);

        // when, then
        assertThatThrownBy(() -> loanService.loanBook(MEMBER_ID, 999L))
                .extracting("errorCode").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    private Loan activeLoan(LocalDateTime loanedAt) {
        Loan loan = Loan.start(MEMBER_ID, 70L, loanedAt, 14);
        ReflectionTestUtils.setField(loan, "id", 100L);
        given(loanRepository.findById(100L)).willReturn(Optional.of(loan));
        given(bookCopyRepository.findById(70L)).willReturn(Optional.of(copy));
        copy.lend();
        return loan;
    }

    @Test
    @DisplayName("3일 늦게 반납하면 3일 동안 대출이 정지되고, 대기자가 없으면 권은 대출 가능으로 돌아간다")
    void returnByMember_연체반납_정지() {
        // given: 반납 예정일이 오늘보다 3일 전
        Loan loan = activeLoan(LocalDateTime.now(CLOCK).minusDays(17));

        // when
        loanService.returnByMember(MEMBER_ID, 100L);

        // then
        assertThat(loan.getStatus()).isEqualTo(com.ryu.library.loan.domain.LoanStatus.RETURNED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getSuspendedUntil()).isEqualTo(TODAY.plusDays(3));
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("반납한 권은 가장 먼저 예약한 대기자 몫으로 보관하고, 대기자는 3일 안에 찾아가야 한다")
    void returnByMember_대기자있음_예약보관() {
        // given
        activeLoan(LocalDateTime.now(CLOCK).minusDays(3));
        Reservation waiting = Reservation.create(2L, copy.getBook().getId(), LocalDateTime.now(CLOCK).minusDays(1));
        given(reservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(any(), any())).willReturn(Optional.of(waiting));

        // when
        loanService.returnByMember(MEMBER_ID, 100L);

        // then
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.RESERVED);
        assertThat(waiting.getStatus()).isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(waiting.getExpiresAt()).isEqualTo(LocalDateTime.now(CLOCK).plusDays(3));
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("다른 회원의 대출은 반납할 수 없고 '없음'과 똑같이 처리한다")
    void returnByMember_남의대출_L004() {
        // given
        activeLoan(LocalDateTime.now(CLOCK).minusDays(3));

        // when, then
        assertThatThrownBy(() -> loanService.returnByMember(999L, 100L))
                .extracting("errorCode").isEqualTo(ErrorCode.LOAN_NOT_FOUND);
    }

    @Test
    @DisplayName("연장하면 반납 예정일이 7일 늘고, 두 번째 연장은 거부한다")
    void extend_1회만() {
        // given
        Loan loan = activeLoan(LocalDateTime.now(CLOCK).minusDays(3));
        LocalDate due = loan.getDueDate();

        // when
        loanService.extend(MEMBER_ID, 100L);

        // then
        assertThat(loan.getDueDate()).isEqualTo(due.plusDays(7));
        assertThatThrownBy(() -> loanService.extend(MEMBER_ID, 100L))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTEND_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("이 도서를 기다리는 예약자가 있거나 연체 중이면 연장할 수 없다")
    void extend_예약대기_연체_거부() {
        // given
        activeLoan(LocalDateTime.now(CLOCK).minusDays(20));
        given(reservationRepository.existsByBookIdAndStatus(any(), any())).willReturn(false);

        // when, then
        assertThatThrownBy(() -> loanService.extend(MEMBER_ID, 100L))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTEND_NOT_ALLOWED_OVERDUE);
        given(reservationRepository.existsByBookIdAndStatus(any(), any())).willReturn(true);
        assertThatThrownBy(() -> loanService.extend(MEMBER_ID, 100L))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTEND_NOT_ALLOWED_RESERVED);
    }

    @Test
    @DisplayName("수령 가능한 예약이 있으면 나를 위해 보관 중인 권을 대출하고 예약을 완료한다")
    void loanBook_수령가능예약_보관권대출() {
        // given
        Reservation notified = Reservation.create(MEMBER_ID, BOOK_ID, LocalDateTime.now(CLOCK).minusDays(2));
        notified.notifyAvailable(LocalDateTime.now(CLOCK).minusDays(1), 3);
        copy.holdForReservation();
        given(reservationRepository.findFirstByMemberIdAndBookIdAndStatusIn(any(), any(), anyCollection()))
                .willReturn(Optional.of(notified));
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(BOOK_ID, BookCopyStatus.RESERVED)).willReturn(List.of(copy));

        // when
        loanService.loanBook(MEMBER_ID, BOOK_ID);

        // then
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
        assertThat(notified.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }
}
