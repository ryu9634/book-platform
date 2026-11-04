package com.ryu.ebook.ebook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EbookTest {

    @Test
    @DisplayName("판매가는 정가보다 클 수 없고, 할인율은 정가와 판매가로 계산한다")
    void 가격검증_할인율() {
        assertThat(EbookFixture.ebook("9780000000001", "제목", 15000, 13500).discountRate()).isEqualTo(10);
        assertThatThrownBy(() -> EbookFixture.ebook("9780000000002", "제목", 10000, 12000))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PRICE);
    }

    @Test
    @DisplayName("새 전자책은 판매 중으로 시작하고, 판매 수는 0 아래로 내려가지 않는다")
    void 판매상태_판매수() {
        Ebook ebook = EbookFixture.ebook("9780000000003", "제목", 10000, 9000);
        assertThat(ebook.isOnSale()).isTrue();
        ebook.decreaseSales();
        assertThat(ebook.getSalesCount()).isZero();
        ebook.increaseSales();
        assertThat(ebook.getSalesCount()).isEqualTo(1);
    }
}
