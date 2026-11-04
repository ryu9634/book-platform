package com.ryu.ebook.user.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.cart.domain.Cart;
import com.ryu.ebook.cart.repository.CartRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.cart.dto.CartResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CartServiceTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final EbookRepository ebookRepository = mock(EbookRepository.class);
    private final BookshelfRepository bookshelfRepository = mock(BookshelfRepository.class);
    private final CartService service = new CartService(cartRepository, ebookRepository, bookshelfRepository);
    private Cart cart;
    private Ebook ebook;

    @BeforeEach
    void setUp() {
        cart = Cart.create(5L);
        ebook = EbookFixture.ebook("9780000000001", "이펙티브 자바", 36000, 32400);
        ReflectionTestUtils.setField(ebook, "id", 1L);
        given(cartRepository.findByMemberId(5L)).willReturn(Optional.of(cart));
        given(ebookRepository.findById(1L)).willReturn(Optional.of(ebook));
    }

    @Test
    @DisplayName("판매 중이고 보유하지 않은 전자책만 담고, 판매 중지나 보유 중이면 거부한다")
    void add_조건() {
        service.add(5L, 1L);
        assertThat(cart.contains(1L)).isTrue();

        given(bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(5L, 1L)).willReturn(true);
        assertThatThrownBy(() -> service.add(5L, 1L)).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_OWNED);

        ebook.changeStatus(EbookStatus.STOP_SALE);
        assertThatThrownBy(() -> service.add(5L, 1L)).extracting("errorCode").isEqualTo(ErrorCode.EBOOK_NOT_ON_SALE);
    }

    @Test
    @DisplayName("장바구니 합계는 판매 중인 항목의 판매가만 더한다")
    void get_합계() {
        Ebook stopped = EbookFixture.ebook("9780000000002", "판매 중지 책", 20000, 18000);
        ReflectionTestUtils.setField(stopped, "id", 2L);
        stopped.changeStatus(EbookStatus.STOP_SALE);
        cart.add(1L);
        cart.add(2L);
        given(ebookRepository.findAllById(anyCollection())).willReturn(List.of(ebook, stopped));

        CartResponse response = service.get(5L);

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalSalePrice()).isEqualTo(32400);
    }
}
