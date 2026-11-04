package com.ryu.ebook.user.bookshelf.controller;

import com.ryu.ebook.bookshelf.repository.BookshelfView;
import com.ryu.ebook.bookshelf.service.ReadingService;
import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookshelf")
public class BookshelfApiController {

    private final BookshelfQueryService bookshelfQueryService;
    private final ReadingService readingService;

    @GetMapping
    public ApiResponse<List<BookshelfView>> bookshelf(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookshelfQueryService.myBookshelf(Long.valueOf(jwt.getSubject())));
    }

    // 페이지 내용을 돌려주고 읽은 위치를 기록합니다.
    @GetMapping("/{bookshelfId}/content")
    public ApiResponse<ReadingService.ReadingPage> content(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookshelfId,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(readingService.read(Long.valueOf(jwt.getSubject()), bookshelfId, page));
    }
}
