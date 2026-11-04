package com.ryu.ebook.bookshelf.repository;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookshelfRepository extends JpaRepository<Bookshelf, Long> {

    boolean existsByMemberIdAndEbookIdAndActiveTrue(Long memberId, Long ebookId);

    Optional<Bookshelf> findByMemberIdAndEbookId(Long memberId, Long ebookId);

    List<Bookshelf> findByMemberIdAndEbookIdInAndActiveTrue(Long memberId, Collection<Long> ebookIds);

    Optional<Bookshelf> findByOrderItemId(Long orderItemId);
}
