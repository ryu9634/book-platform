package com.ryu.ebook.review.repository;

import com.ryu.ebook.review.domain.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByEbookIdAndMemberId(Long ebookId, Long memberId);

    Optional<Review> findByEbookIdAndMemberId(Long ebookId, Long memberId);

    @Query(value = """
            select new com.ryu.ebook.review.repository.ReviewView(r.id, m.id, m.name, r.rating, r.content, r.createdAt, r.updatedAt)
            from Review r join Member m on m.id = r.memberId
            where r.ebookId = :ebookId
            """,
            countQuery = "select count(r) from Review r where r.ebookId = :ebookId")
    Page<ReviewView> findViewsByEbook(@Param("ebookId") Long ebookId, Pageable pageable);

    @Query("select new com.ryu.ebook.review.repository.ReviewSummary(coalesce(avg(r.rating), 0), count(r)) from Review r where r.ebookId = :ebookId")
    ReviewSummary summarize(@Param("ebookId") Long ebookId);
}
