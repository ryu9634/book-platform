package com.ryu.ebook.review.repository;

import java.time.LocalDateTime;

// 리뷰 목록용 조회 결과. 작성자 이름은 회원을 조인해 한 번에 가져옵니다.
public record ReviewView(Long reviewId, Long memberId, String memberName, int rating, String content,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
