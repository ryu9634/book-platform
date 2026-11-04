package com.ryu.ebook.ebook.domain;

// ON_SALE 판매 중, STOP_SALE 판매 중지(이미 산 회원은 계속 열람), HIDDEN 목록과 상세에서 숨김
public enum EbookStatus {
    ON_SALE,
    STOP_SALE,
    HIDDEN
}
