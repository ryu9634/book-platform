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
    private final LibraryPolicy policy = new LibraryPolicy(5, 14, 7, 1, 3);
    private LoanService loanService;
    private Member member;
    private BookCopy copy;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookRepository, bookCopyRepository, memberRepository, policy, CLOCK);
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
}
