package com.ryu.library.loan.repository;

// 기간 안 대출 수 순위. 다른 서비스(ebook)에 공개하는 값이라 회원 정보는 넣지 않습니다.
public record PopularBookView(Long bookId, String isbn, String title, String author, long loanCount) {
}
