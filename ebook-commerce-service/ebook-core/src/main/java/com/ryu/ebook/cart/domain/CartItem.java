package com.ryu.ebook.cart.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 항목. 수량 없이 전자책 한 권이 한 줄입니다.
@Getter
@Entity
@Table(name = "CART_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CART_ITEM_SEQ_GENERATOR")
    @SequenceGenerator(name = "CART_ITEM_SEQ_GENERATOR", sequenceName = "CART_ITEM_SEQ", allocationSize = 50)
    @Column(name = "CART_ITEM_ID")
    private Long id;

    // 같은 장바구니 애그리거트 안이라 객체로 참조합니다(FK 없음).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CART_ID", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Cart cart;

    // 전자책은 다른 애그리거트라 ID로만 참조합니다.
    @Column(nullable = false)
    private Long ebookId;

    CartItem(Cart cart, Long ebookId) {
        this.cart = cart;
        this.ebookId = ebookId;
    }
}
