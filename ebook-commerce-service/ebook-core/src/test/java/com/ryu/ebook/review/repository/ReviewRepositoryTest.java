package com.ryu.ebook.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.review.domain.Review;
import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ReviewRepositoryTest {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("리뷰 목록은 작성자 이름을 조인해 가져오고, 평균 별점과 개수를 한 번에 계산하며, 같은 회원의 두 번째 리뷰는 DB가 막는다")
    void 조회_집계_중복() {
        // given
        var a = memberRepository.save(MemberFixture.activeMember("rv01", "rv01@example.com"));
        var b = memberRepository.save(MemberFixture.activeMember("rv02", "rv02@example.com"));
        reviewRepository.save(Review.write(10L, a.getId(), 5, "좋아요"));
        reviewRepository.saveAndFlush(Review.write(10L, b.getId(), 4, "괜찮아요"));

        // when, then
        assertThat(reviewRepository.findViewsByEbook(10L, PageRequest.of(0, 10)).getContent())
                .extracting(ReviewView::memberName).containsOnly("홍길동");
        ReviewSummary summary = reviewRepository.summarize(10L);
        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.averageText()).isEqualTo("4.5");
        assertThat(reviewRepository.summarize(999L).count()).isZero();
        assertThatThrownBy(() -> reviewRepository.saveAndFlush(Review.write(10L, a.getId(), 3, "또 씀")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
