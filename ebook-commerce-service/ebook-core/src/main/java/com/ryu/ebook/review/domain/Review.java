package com.ryu.ebook.review.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰: 전자책당 회원 한 명이 하나만 씁니다((EBOOK_ID, MEMBER_ID) UNIQUE). 구매 여부는 서비스에서 확인합니다.
@Getter
@Entity
@Table(name = "REVIEW")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REVIEW_SEQ_GENERATOR")
    @SequenceGenerator(name = "REVIEW_SEQ_GENERATOR", sequenceName = "REVIEW_SEQ", allocationSize = 50)
    @Column(name = "REVIEW_ID")
    private Long id;

    @Column(nullable = false)
    private Long ebookId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 2000)
    private String content;

    private Review(Long ebookId, Long memberId, int rating, String content) {
        validate(rating, content);
        this.ebookId = ebookId;
        this.memberId = memberId;
        this.rating = rating;
        this.content = content.trim();
    }

    public static Review write(Long ebookId, Long memberId, int rating, String content) {
        return new Review(ebookId, memberId, rating, content);
    }

    public void edit(int rating, String content) {
        validate(rating, content);
        this.rating = rating;
        this.content = content.trim();
    }

    public boolean isWrittenBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    // DB에도 같은 CHECK 제약(1~5)이 있지만, 화면에 이유를 보여 주기 위해 먼저 확인합니다.
    private static void validate(int rating, String content) {
        if (rating < 1 || rating > 5 || content == null || content.isBlank() || content.trim().length() > 2000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
