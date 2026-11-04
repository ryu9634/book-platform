package com.ryu.ebook.user.ebook.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.global.common.PageResponse;
import com.ryu.ebook.user.ebook.dto.EbookDetailResponse;
import com.ryu.ebook.user.ebook.dto.EbookSort;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ebooks")
public class EbookApiController {

    private final EbookQueryService ebookQueryService;

    @GetMapping
    public ApiResponse<PageResponse<EbookSummaryResponse>> search(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId, @RequestParam(defaultValue = "POPULAR") EbookSort sort,
            Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(ebookQueryService.search(keyword, categoryId, sort, pageable)));
    }

    @GetMapping("/{ebookId}")
    public ApiResponse<EbookDetailResponse> detail(@PathVariable Long ebookId) {
        return ApiResponse.ok(ebookQueryService.getEbook(ebookId));
    }
}
