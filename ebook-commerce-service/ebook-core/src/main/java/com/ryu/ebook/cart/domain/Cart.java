package com.ryu.ebook.cart.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원당 장바구니 하나. 가입할 때 함께 만듭니다. 담긴 항목(CART_ITEM)은 장바구니 기능에서 추가합니다.
@Getter
@Entity
@Table(name = "CART")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CART_SEQ_GENERATOR")
    @SequenceGenerator(name = "CART_SEQ_GENERATOR", sequenceName = "CART_SEQ", allocationSize = 50)
    @Column(name = "CART_ID")
    private Long id;

    // 회원은 다른 애그리거트라 ID로만 참조합니다. MEMBER_ID에는 UNIQUE 제약이 있습니다.
    @Column(nullable = false)
    private Long memberId;

    private Cart(Long memberId) {
        this.memberId = memberId;
    }

    public static Cart create(Long memberId) {
        return new Cart(memberId);
    }
}
