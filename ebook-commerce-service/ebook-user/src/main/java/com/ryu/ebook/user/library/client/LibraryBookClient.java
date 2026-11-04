package com.ryu.ebook.user.library.client;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

// 도서관 서비스(library-user)의 공개 API. 인터페이스만 선언하면 Spring이 HTTP 호출 구현을 만듭니다(HTTP Interface).
@HttpExchange("/api/v1/books")
public interface LibraryBookClient {

    @GetExchange("/popular")
    PopularBooksResponse popular(@RequestParam("limit") int limit);

    // 도서관 서비스의 ApiResponse 형식 중 쓰는 부분만 받습니다.
    record PopularBooksResponse(boolean success, List<PopularBook> data) {
    }

    record PopularBook(Long bookId, String isbn, String title, String author, long loanCount) {
    }
}
