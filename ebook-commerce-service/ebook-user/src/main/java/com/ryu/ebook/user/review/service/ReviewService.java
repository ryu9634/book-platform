package com.ryu.ebook.user.review.service;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.review.domain.Review;
import com.ryu.ebook.review.repository.ReviewRepository;
import com.ryu.ebook.review.repository.ReviewSummary;
import com.ryu.ebook.review.repository.ReviewView;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookshelfRepository bookshelfRepository;
    private final EbookRepository ebookRepository;

    // 쓰기 조건: 내 서재에 있는(환불하지 않은) 전자책, 아직 리뷰를 쓰지 않음.
    // 동시에 두 번 쓰면 (EBOOK_ID, MEMBER_ID) UNIQUE 제약이 막습니다.
    @Transactional
    public Long write(Long memberId, Long ebookId, int rating, String content) {
        if (!ebookRepository.existsById(ebookId)) {
            throw new BusinessException(ErrorCode.EBOOK_NOT_FOUND);
        }
        if (!bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(memberId, ebookId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        if (reviewRepository.existsByEbookIdAndMemberId(ebookId, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }
        Long id = reviewRepository.save(Review.write(ebookId, memberId, rating, content)).getId();
        log.info("review written. reviewId={}, ebookId={}", id, ebookId);
        return id;
    }

    @Transactional
    public Long edit(Long memberId, Long reviewId, int rating, String content) {
        Review review = findOwned(memberId, reviewId);
        review.edit(rating, content);
        return review.getEbookId();
    }

    // 리뷰는 다른 데이터가 참조하지 않아 물리 삭제합니다.
    @Transactional
    public Long delete(Long memberId, Long reviewId) {
        Review review = findOwned(memberId, reviewId);
        reviewRepository.delete(review);
        return review.getEbookId();
    }

    public Review get(Long memberId, Long reviewId) {
        return findOwned(memberId, reviewId);
    }

    public Page<ReviewView> reviews(Long ebookId, Pageable pageable) {
        return reviewRepository.findViewsByEbook(ebookId, pageable);
    }

    public ReviewSummary summary(Long ebookId) {
        return reviewRepository.summarize(ebookId);
    }

    public Optional<Review> myReview(Long memberId, Long ebookId) {
        return reviewRepository.findByEbookIdAndMemberId(ebookId, memberId);
    }

    public boolean canWrite(Long memberId, Long ebookId) {
        return bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(memberId, ebookId)
                && !reviewRepository.existsByEbookIdAndMemberId(ebookId, memberId);
    }

    // 다른 회원의 리뷰는 "없음"과 똑같이 처리합니다.
    private Review findOwned(Long memberId, Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(review -> review.isWrittenBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }
}
