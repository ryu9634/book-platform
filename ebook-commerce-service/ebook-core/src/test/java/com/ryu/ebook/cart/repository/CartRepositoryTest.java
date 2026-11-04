package com.ryu.ebook.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.cart.domain.Cart;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class CartRepositoryTest {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("장바구니에 담은 항목은 장바구니를 저장할 때 함께 저장되고, 빼면 DB에서도 지워진다")
    void cascade_orphanRemoval() {
        // given
        Cart cart = cartRepository.save(Cart.create(100L));
        cart.add(10L);
        cart.add(11L);
        em.flush();
        em.clear();

        // when
        Cart loaded = cartRepository.findByMemberId(100L).orElseThrow();
        loaded.removeEbooks(java.util.List.of(10L));
        em.flush();
        em.clear();

        // then
        assertThat(cartRepository.findByMemberId(100L).orElseThrow().getItems()).hasSize(1);
    }
}
