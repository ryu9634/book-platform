package com.ryu.ebook.cart.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원당 장바구니 하나. 가입할 때 함께 만듭니다.
// 항목은 장바구니 없이 존재할 수 없어 같은 애그리거트로 묶고, 장바구니를 통해서만 추가·삭제합니다(cascade, orphanRemoval).
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

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Cart(Long memberId) {
        this.memberId = memberId;
    }

    public static Cart create(Long memberId) {
        return new Cart(memberId);
    }

    public boolean contains(Long ebookId) {
        return items.stream().anyMatch(item -> item.getEbookId().equals(ebookId));
    }

    // 같은 전자책은 한 번만 담깁니다. 동시에 담아도 (CART_ID, EBOOK_ID) UNIQUE 제약이 막습니다.
    public CartItem add(Long ebookId) {
        if (contains(ebookId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_CART);
        }
        CartItem item = new CartItem(this, ebookId);
        items.add(item);
        return item;
    }

    public void remove(Long cartItemId) {
        if (!items.removeIf(item -> cartItemId.equals(item.getId()))) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    // 주문을 만들면 주문한 전자책을 장바구니에서 뺍니다.
    public void removeEbooks(Collection<Long> ebookIds) {
        items.removeIf(item -> ebookIds.contains(item.getEbookId()));
    }
}
