package com.ryu.ebook.user.ebook.dto;

// 전자책 상세의 버튼 분기: 보유(서재에서 읽기) → 장바구니에 있음 → 구매 가능
public enum PurchaseState {
    OWNED,
    IN_CART,
    CAN_PURCHASE
}
