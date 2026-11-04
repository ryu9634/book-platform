package com.ryu.ebook.bookshelf.repository;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookshelfRepository extends JpaRepository<Bookshelf, Long> {

    boolean existsByMemberIdAndEbookIdAndActiveTrue(Long memberId, Long ebookId);

    Optional<Bookshelf> findByMemberIdAndEbookId(Long memberId, Long ebookId);

    List<Bookshelf> findByMemberIdAndEbookIdInAndActiveTrue(Long memberId, Collection<Long> ebookIds);

    Optional<Bookshelf> findByOrderItemId(Long orderItemId);

    List<Bookshelf> findByOrderItemIdIn(Collection<Long> orderItemIds);

    // 최근에 읽은 책이 먼저, 읽지 않은 책은 산 순서대로 뒤에 둡니다.
    @Query("""
            select new com.ryu.ebook.bookshelf.repository.BookshelfView(s.id, e.id, e.title, e.author, e.coverImageUrl,
                   e.pageCount, s.lastReadPage, s.lastReadAt, s.acquiredAt)
            from Bookshelf s join Ebook e on e.id = s.ebookId
            where s.memberId = :memberId and s.active = true
            order by s.lastReadAt desc nulls last, s.acquiredAt desc
            """)
    List<BookshelfView> findViewsByMember(@Param("memberId") Long memberId);
}
