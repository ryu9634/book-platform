package com.ryu.ebook.user.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.review.domain.Review;
import com.ryu.ebook.review.repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final BookshelfRepository bookshelfRepository = mock(BookshelfRepository.class);
    private final EbookRepository ebookRepository = mock(EbookRepository.class);
    private final ReviewService service = new ReviewService(reviewRepository, bookshelfRepository, ebookRepository);

    @BeforeEach
    void setUp() {
        given(ebookRepository.existsById(10L)).willReturn(true);
        given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("구매한 회원만, 한 번만 리뷰를 쓸 수 있다")
    void write_구매자만_한번만() {
        assertThatThrownBy(() -> service.write(5L, 10L, 5, "좋아요"))
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);

        given(bookshelfRepository.existsByMemberIdAndEbookIdAndActiveTrue(5L, 10L)).willReturn(true);
        service.write(5L, 10L, 5, "좋아요");

        given(reviewRepository.existsByEbookIdAndMemberId(10L, 5L)).willReturn(true);
        assertThatThrownBy(() -> service.write(5L, 10L, 5, "또 씀"))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_REVIEW);
    }

    @Test
    @DisplayName("다른 회원의 리뷰는 수정·삭제할 수 없고 '없음'과 똑같이 처리한다")
    void 남의리뷰_RV003() {
        given(reviewRepository.findById(1L)).willReturn(Optional.of(Review.write(10L, 7L, 4, "남의 리뷰")));

        assertThatThrownBy(() -> service.edit(5L, 1L, 1, "바꿈")).extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(5L, 1L)).extracting("errorCode").isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("별점은 1~5점, 내용은 비어 있을 수 없다")
    void 검증() {
        Review review = Review.write(10L, 5L, 3, "  보통  ");
        assertThat(review.getContent()).isEqualTo("보통");
        assertThatThrownBy(() -> review.edit(6, "x")).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> review.edit(3, " ")).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
