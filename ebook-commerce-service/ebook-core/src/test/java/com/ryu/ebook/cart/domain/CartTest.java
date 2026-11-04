package com.ryu.ebook.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartTest {

    @Test
    @DisplayName("같은 전자책은 한 번만 담기고, 주문한 전자책은 장바구니에서 빠진다")
    void add_중복_removeEbooks() {
        Cart cart = Cart.create(1L);
        cart.add(10L);
        cart.add(11L);

        assertThatThrownBy(() -> cart.add(10L)).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_IN_CART);
        cart.removeEbooks(List.of(10L));
        assertThat(cart.getItems()).extracting(CartItem::getEbookId).containsExactly(11L);
    }
}
