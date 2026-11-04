package com.ryu.library.book.repository;

import com.ryu.library.book.domain.Book;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByCategoryId(Long categoryId);

    Page<Book> findByTitleContaining(String keyword, Pageable pageable);

    List<Book> findTop8ByOrderByIdDesc();

    // 검색어(제목 또는 저자)와 분류는 둘 다 선택 조건입니다. 조건이 2개를 넘어 메서드 이름 쿼리 대신 JPQL로 씁니다.
    @Query(value = """
            select b from Book b
            where (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%'))
                                    or lower(b.author) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or b.categoryId = :categoryId)
            """,
            countQuery = """
            select count(b) from Book b
            where (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%'))
                                    or lower(b.author) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or b.categoryId = :categoryId)
            """)
    Page<Book> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);
}
