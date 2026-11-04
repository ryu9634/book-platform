package com.ryu.ebook.user.ebook.dto;

import org.springframework.data.domain.Sort;

// 목록 정렬. 화면에서 받은 값을 정해진 정렬로만 바꿔 임의의 컬럼으로 정렬하지 못하게 합니다.
public enum EbookSort {
    POPULAR(Sort.by(Sort.Order.desc("salesCount"), Sort.Order.desc("id"))),
    NEWEST(Sort.by(Sort.Order.desc("id"))),
    PRICE_ASC(Sort.by(Sort.Order.asc("salePrice"), Sort.Order.desc("id"))),
    PRICE_DESC(Sort.by(Sort.Order.desc("salePrice"), Sort.Order.desc("id")));

    private final Sort sort;

    EbookSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
