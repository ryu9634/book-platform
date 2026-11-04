package com.ryu.library.user.loan.dto;

// bookId(도서 단위, 대출 가능한 권 자동 선택) 또는 bookCopyId(권 단위) 중 하나를 보냅니다.
public record LoanRequest(Long bookId, Long bookCopyId) {

    public boolean isValid() {
        return (bookId == null) != (bookCopyId == null);
    }
}
