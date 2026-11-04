package com.ryu.library.admin.book.dto;

import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;

public record BookCopyResponse(Long id, String barcode, BookCopyStatus status) {

    public static BookCopyResponse from(BookCopy copy) {
        return new BookCopyResponse(copy.getId(), copy.getBarcode(), copy.getStatus());
    }

    public String statusLabel() {
        return switch (status) {
            case AVAILABLE -> "대출 가능";
            case ON_LOAN -> "대출 중";
            case RESERVED -> "예약 보관";
            case LOST -> "분실";
            case DISCARDED -> "폐기";
        };
    }
}
