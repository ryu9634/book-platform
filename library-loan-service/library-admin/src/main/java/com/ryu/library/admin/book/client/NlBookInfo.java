package com.ryu.library.admin.book.client;

import java.time.LocalDate;

// 국립중앙도서관에서 받은 서지 정보를 우리 도서 등록에 필요한 형태로 정리한 값.
public record NlBookInfo(
        String isbn,
        String title,
        String author,
        String publisher,
        LocalDate publishedDate,
        String coverImageUrl,
        String description,
        String kdc) {
}
