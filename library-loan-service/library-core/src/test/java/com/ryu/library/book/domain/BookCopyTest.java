package com.ryu.library.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.book.BookFixture;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookCopyTest {

    private BookCopy newCopy() {
        return BookCopy.create(BookFixture.book("9780000000001", "제목"), "LIB-000001-001");
    }

    @Test
    @DisplayName("새 권은 대출 가능 상태로 시작하고, 바코드는 도서 ID와 권 번호로 만든다")
    void create_AVAILABLE_바코드형식() {
        assertThat(newCopy().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
        assertThat(BookCopy.barcodeOf(12L, 3)).isEqualTo("LIB-000012-003");
    }

    @Test
    @DisplayName("대출 가능한 권만 대출되고, 이미 대출 중인 권은 B008로 거부한다")
    void lend_대출중_B008() {
        // given
        BookCopy copy = newCopy();
        copy.lend();

        // when, then
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
        assertThatThrownBy(copy::lend)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COPY_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("대출 중인 권은 분실 처리할 수 없다 (반납 후 처리)")
    void markLost_대출중_B009() {
        // given
        BookCopy copy = newCopy();
        copy.lend();

        // when, then
        assertThatThrownBy(copy::markLost)
                .extracting("errorCode").isEqualTo(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("분실된 권은 찾으면 다시 대출 가능, 폐기하면 되돌릴 수 없다")
    void 분실_발견_폐기() {
        // given
        BookCopy copy = newCopy();
        copy.markLost();

        // when
        copy.restoreFound();
        copy.discard();

        // then
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.DISCARDED);
        assertThatThrownBy(copy::restoreFound)
                .extracting("errorCode").isEqualTo(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("예약자용으로 잡아 둔 권은 일반 대출이 안 되고 예약자 대출로만 나간다")
    void 예약보관_예약자대출만() {
        // given
        BookCopy copy = newCopy();
        copy.holdForReservation();

        // when, then
        assertThatThrownBy(copy::lend).extracting("errorCode").isEqualTo(ErrorCode.COPY_NOT_AVAILABLE);
        copy.lendToReserver();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
    }
}
