package com.ryu.ebook.user.review.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.global.common.PageResponse;
import com.ryu.ebook.review.repository.ReviewView;
import com.ryu.ebook.user.review.dto.ReviewForm;
import com.ryu.ebook.user.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewApiController {

    private final ReviewService reviewService;

    @GetMapping("/ebooks/{ebookId}/reviews")
    public ApiResponse<PageResponse<ReviewView>> reviews(@PathVariable Long ebookId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(reviewService.reviews(ebookId, pageable)));
    }

    @PostMapping("/ebooks/{ebookId}/reviews")
    public ResponseEntity<ApiResponse<Map<String, Long>>> write(@AuthenticationPrincipal Jwt jwt, @PathVariable Long ebookId,
            @Valid @RequestBody ReviewForm request) {
        Long id = reviewService.write(Long.valueOf(jwt.getSubject()), ebookId, request.getRating(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("reviewId", id)));
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<Void> edit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reviewId,
            @Valid @RequestBody ReviewForm request) {
        reviewService.edit(Long.valueOf(jwt.getSubject()), reviewId, request.getRating(), request.getContent());
        return ApiResponse.ok();
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reviewId) {
        reviewService.delete(Long.valueOf(jwt.getSubject()), reviewId);
        return ApiResponse.ok();
    }
}
