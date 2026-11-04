package com.ryu.library.user.book.controller;

import com.ryu.library.global.common.ApiResponse;
import com.ryu.library.global.common.PageResponse;
import com.ryu.library.user.book.dto.BookDetailResponse;
import com.ryu.library.user.book.dto.BookSummaryResponse;
import com.ryu.library.user.book.service.BookQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
public class BookApiController {

    private final BookQueryService bookQueryService;

    @GetMapping
    public ApiResponse<PageResponse<BookSummaryResponse>> search(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(bookQueryService.search(keyword, categoryId, pageable)));
    }

    @GetMapping("/{bookId}")
    public ApiResponse<BookDetailResponse> detail(@PathVariable Long bookId) {
        return ApiResponse.ok(bookQueryService.getBook(bookId));
    }
}
