package com.ryu.ebook.user.cart.service;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.cart.domain.Cart;
import com.ryu.ebook.cart.domain.CartItem;
import com.ryu.ebook.cart.repository.CartRepository;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.cart.dto.CartResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final EbookRepository ebookRepository;
    private final BookshelfRepository bookshelfRepository;

    // 담기 조건: 판매 중, 아직 내 서재에 없음, 장바구니에 없음
    @Transactional
    public void add(Long memberId, Long ebookId) {
        Ebook ebook = ebookRepository.findById(ebookId)
                .filter(e -> !e.isHidden())
                .orElseThrow(() -> new BusinessException(ErrorCode.EBOOK_NOT_FOUND));
        if (!ebook.isOnSale()) {
            throw new BusinessException(ErrorCode.EBOOK_NOT_ON_SALE);
        }
        if (bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(memberId, ebookId)) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED);
        }
        CartItem item = cart(memberId).add(ebookId);
        log.info("cart item added. memberId={}, ebookId={}", memberId, item.getEbookId());
    }

    @Transactional
    public void remove(Long memberId, Long cartItemId) {
        cart(memberId).remove(cartItemId);
    }

    // 항목 수만큼 전자책을 따로 조회하지 않도록 한 번에 읽습니다(N+1 방지).
    public CartResponse get(Long memberId) {
        Cart cart = cart(memberId);
        List<Long> ebookIds = cart.getItems().stream().map(CartItem::getEbookId).toList();
        Map<Long, Ebook> ebooks = ebookRepository.findAllById(ebookIds).stream()
                .collect(Collectors.toMap(Ebook::getId, Function.identity()));
        List<CartResponse.Item> items = cart.getItems().stream()
                .filter(item -> ebooks.containsKey(item.getEbookId()))
                .map(item -> {
                    Ebook e = ebooks.get(item.getEbookId());
                    return new CartResponse.Item(item.getId(), e.getId(), e.getTitle(), e.getAuthor(),
                            e.getCoverImageUrl(), e.getPrice(), e.getSalePrice(), e.isOnSale());
                })
                .toList();
        return CartResponse.of(items);
    }

    public boolean contains(Long memberId, Long ebookId) {
        return cart(memberId).contains(ebookId);
    }

    private Cart cart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
