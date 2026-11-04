package com.ryu.library.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.global.policy.LibraryPolicy;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.domain.ReservationStatus;
import com.ryu.library.reservation.repository.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReservationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final LibraryPolicy policy = new LibraryPolicy(5, 14, 7, 1, 3);
    private ReservationService service;
    private Book book;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, bookRepository, bookCopyRepository, loanRepository,
                memberRepository, new ReservationHandOver(reservationRepository, policy, CLOCK), CLOCK);
        book = BookFixture.book("9780000000081", "제목");
        ReflectionTestUtils.setField(book, "id", 7L);
        copy = BookCopy.create(book, "LIB-000007-001");
        given(bookRepository.existsById(7L)).willReturn(true);
        given(memberRepository.findById(1L)).willReturn(Optional.of(MemberFixture.activeMember()));
        given(bookCopyRepository.findByBookIdOrderByIdAsc(7L)).willReturn(List.of(copy));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("모든 권이 대출 중이면 예약할 수 있다")
    void reserve_모두대출중_성공() {
        copy.lend();
        service.reserve(1L, 7L);
    }

    @Test
    @DisplayName("대출 가능한 권이 있으면 예약하지 말고 바로 대출하라고 안내한다")
    void reserve_가용권있음_R001() {
        assertThatThrownBy(() -> service.reserve(1L, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_NOT_ALLOWED_AVAILABLE);
    }

    @Test
    @DisplayName("이미 빌린 도서거나 이미 예약한 도서는 예약할 수 없다")
    void reserve_대출중_중복_거부() {
        copy.lend();
        given(loanRepository.existsActiveLoanOfBook(1L, 7L)).willReturn(true);
        assertThatThrownBy(() -> service.reserve(1L, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_NOT_ALLOWED_LOANED);

        given(loanRepository.existsActiveLoanOfBook(1L, 7L)).willReturn(false);
        given(reservationRepository.existsByMemberIdAndBookIdAndStatusIn(any(), any(), anyCollection())).willReturn(true);
        assertThatThrownBy(() -> service.reserve(1L, 7L))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_RESERVATION);
    }

    @Test
    @DisplayName("수령 가능 상태에서 취소하면 보관하던 권을 다음 대기자에게 넘긴다")
    void cancel_수령가능_다음대기자에게() {
        // given
        Reservation mine = Reservation.create(1L, 7L, NOW.minusDays(2));
        mine.notifyAvailable(NOW.minusDays(1), 3);
        Reservation next = Reservation.create(2L, 7L, NOW.minusDays(1));
        copy.holdForReservation();
        given(reservationRepository.findById(10L)).willReturn(Optional.of(mine));
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(7L, BookCopyStatus.RESERVED)).willReturn(List.of(copy));
        given(reservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAscIdAsc(7L, ReservationStatus.WAITING))
                .willReturn(Optional.of(next));

        // when
        service.cancel(1L, 10L);

        // then
        assertThat(mine.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(next.getStatus()).isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.RESERVED);
    }

    @Test
    @DisplayName("수령 기한이 지난 예약은 만료되고, 대기자가 없으면 권은 대출 가능으로 돌아간다")
    void expireOverdueHolds_만료_서가로() {
        // given
        Reservation expired = Reservation.create(1L, 7L, NOW.minusDays(5));
        expired.notifyAvailable(NOW.minusDays(4), 3);
        copy.holdForReservation();
        given(reservationRepository.findByStatusAndExpiresAtLessThanEqual(ReservationStatus.NOTIFIED, NOW))
                .willReturn(List.of(expired));
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(7L, BookCopyStatus.RESERVED)).willReturn(List.of(copy));

        // when
        int count = service.expireOverdueHolds();

        // then
        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("다른 회원의 예약은 취소할 수 없고 '없음'과 똑같이 처리한다")
    void cancel_남의예약_R004() {
        given(reservationRepository.findById(10L)).willReturn(Optional.of(Reservation.create(2L, 7L, NOW)));
        assertThatThrownBy(() -> service.cancel(1L, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }
}
