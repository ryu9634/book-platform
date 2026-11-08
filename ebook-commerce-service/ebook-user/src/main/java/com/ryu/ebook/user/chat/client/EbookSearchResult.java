package com.ryu.ebook.user.chat.client;

import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.util.List;

// 검색 도구가 모델에 돌려주는 값. 토큰을 아끼려고 화면용 필드를 다 보내지 않고 고르는 데 필요한 것만 보냅니다.
public record EbookSearchResult(String notice, List<Book> books) {

    public record Book(String title, String author, long salePrice, String categoryName) {
    }

    public static EbookSearchResult of(List<EbookSummaryResponse> books) {
        List<Book> items = books.stream()
                .map(b -> new Book(b.title(), b.author(), b.salePrice(), b.categoryName()))
                .toList();
        return new EbookSearchResult(items.isEmpty() ? "검색 결과가 없다. 다른 검색어를 쓰거나 없다고 답한다." : null, items);
    }

    public static EbookSearchResult limitReached() {
        return new EbookSearchResult("이 질문에서 쓸 수 있는 검색 횟수를 모두 썼다. 지금까지 찾은 책으로만 답한다.", List.of());
    }
}
