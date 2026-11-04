package com.ryu.library.user.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.library.book.BookFixture;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.MemberFixture;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.reservation.domain.Reservation;
import com.ryu.library.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import com.ryu.library.user.loan.dto.BookActionState;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoanQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final LoanQueryService service =
            new LoanQueryService(loanRepository, bookCopyRepository, memberRepository, reservationRepository, CLOCK);
    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.activeMember();
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
    }

    @Test
    @DisplayName("정지 중이면 다른 조건보다 먼저 정지 상태를 보여 준다")
    void actionState_정지우선() {
        member.suspendUntil(LocalDate.of(2026, 11, 20));
        given(loanRepository.existsActiveLoanOfBook(5L, 1L)).willReturn(true);

        assertThat(service.actionState(5L, 1L).type()).isEqualTo(BookActionState.Type.SUSPENDED);
    }

    @Test
    @DisplayName("이미 빌린 도서면 대출 중, 대출 가능한 권이 있으면 대출 가능, 없으면 모두 대출 중")
    void actionState_분기() {
        given(loanRepository.existsActiveLoanOfBook(5L, 1L)).willReturn(true);
        assertThat(service.actionState(5L, 1L).type()).isEqualTo(BookActionState.Type.ALREADY_LOANED);

        given(loanRepository.existsActiveLoanOfBook(5L, 2L)).willReturn(false);
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(2L, BookCopyStatus.AVAILABLE))
                .willReturn(List.of(BookCopy.create(BookFixture.book("9780000000071", "제목"), "LIB-1")));
        assertThat(service.actionState(5L, 2L).type()).isEqualTo(BookActionState.Type.CAN_LOAN);

        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(3L, BookCopyStatus.AVAILABLE)).willReturn(List.of());
        assertThat(service.actionState(5L, 3L).type()).isEqualTo(BookActionState.Type.NO_AVAILABLE_COPY);
    }

    @Test
    @DisplayName("수령 가능한 예약이 있으면 예약 도서 대출, 대기 중이고 가용 권이 없으면 대기 순번을 보여 준다")
    void actionState_예약분기() {
        // given
        Reservation notified = Reservation.create(5L, 4L, LocalDateTime.now(CLOCK));
        notified.notifyAvailable(LocalDateTime.now(CLOCK), 3);
        given(reservationRepository.findFirstByMemberIdAndBookIdAndStatusIn(org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq(4L), anyCollection())).willReturn(Optional.of(notified));
        assertThat(service.actionState(5L, 4L).type()).isEqualTo(BookActionState.Type.RESERVED_READY);

        Reservation waiting = Reservation.create(5L, 6L, LocalDateTime.now(CLOCK));
        given(reservationRepository.findFirstByMemberIdAndBookIdAndStatusIn(org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq(6L), anyCollection())).willReturn(Optional.of(waiting));
        given(bookCopyRepository.findByBookIdAndStatusOrderByIdAsc(6L, BookCopyStatus.AVAILABLE)).willReturn(List.of());
        given(reservationRepository.waitingPosition(any(), any(), any())).willReturn(2L);
        BookActionState state = service.actionState(5L, 6L);
        assertThat(state.type()).isEqualTo(BookActionState.Type.RESERVED_WAITING);
        assertThat(state.waitingPosition()).isEqualTo(2L);
    }
}
