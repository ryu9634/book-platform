package com.ryu.library.user.book.dto;

import com.ryu.library.book.domain.BookCopyStatus;
import java.util.Map;

// 도서 한 권의 상태별 권 수. 소장 권 수에는 분실, 폐기를 넣지 않습니다.
public record CopyCounts(long available, long holding) {

    public static final CopyCounts NONE = new CopyCounts(0, 0);

    public static CopyCounts from(Map<BookCopyStatus, Long> byStatus) {
        long available = byStatus.getOrDefault(BookCopyStatus.AVAILABLE, 0L);
        long holding = available
                + byStatus.getOrDefault(BookCopyStatus.ON_LOAN, 0L)
                + byStatus.getOrDefault(BookCopyStatus.RESERVED, 0L);
        return new CopyCounts(available, holding);
    }
}
