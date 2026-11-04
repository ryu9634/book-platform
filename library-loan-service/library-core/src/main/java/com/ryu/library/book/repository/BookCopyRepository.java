package com.ryu.library.book.repository;

import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.domain.BookCopyStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookIdOrderByIdAsc(Long bookId);

    long countByBookId(Long bookId);

    List<BookCopy> findByBookIdAndStatusOrderByIdAsc(Long bookId, BookCopyStatus status);

    // 목록 화면에서 도서마다 따로 세지 않도록(N+1) 한 번의 GROUP BY로 상태별 권 수를 셉니다.
    @Query("""
            select c.book.id as bookId, c.status as status, count(c) as count
            from BookCopy c
            where c.book.id in :bookIds
            group by c.book.id, c.status
            """)
    List<CopyCount> countByBookIdsGroupByStatus(@Param("bookIds") Collection<Long> bookIds);

    interface CopyCount {
        Long getBookId();

        BookCopyStatus getStatus();

        long getCount();
    }
}
