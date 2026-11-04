package com.ryu.ebook.user.bookshelf.controller;

import com.ryu.ebook.bookshelf.repository.BookshelfView;
import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookshelf")
public class BookshelfApiController {

    private final BookshelfQueryService bookshelfQueryService;

    @GetMapping
    public ApiResponse<List<BookshelfView>> bookshelf(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(bookshelfQueryService.myBookshelf(Long.valueOf(jwt.getSubject())));
    }
}
