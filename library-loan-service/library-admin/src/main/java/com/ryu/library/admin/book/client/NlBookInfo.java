package com.ryu.library.admin.book.client;

// 국립중앙도서관에서 받은 서지 정보를 우리 도서 등록에 필요한 형태로 정리한 값.
// 발행일은 연도만 제공되므로 날짜를 지어내지 않고 연도만 둡니다. 표지 이미지는 제공되지 않습니다.
public record NlBookInfo(
        String isbn,
        String title,
        String author,
        String publisher,
        Integer publishedYear,
        String description,
        String kdc) {
}
