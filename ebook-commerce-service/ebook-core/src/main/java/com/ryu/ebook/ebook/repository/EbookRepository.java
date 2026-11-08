package com.ryu.ebook.ebook.repository;

import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EbookRepository extends JpaRepository<Ebook, Long> {

    boolean existsByIsbn(String isbn);

    List<Ebook> findByIsbnIn(Collection<String> isbns);

    boolean existsByCategoryId(Long categoryId);

    List<Ebook> findTop8ByStatusOrderBySalesCountDescIdDesc(EbookStatus status);

    List<Ebook> findTop8ByStatusOrderByIdDesc(EbookStatus status);

    // 상태(보여 줄 상태 목록), 검색어(제목·저자), 분류는 선택 조건입니다. 정렬은 Pageable로 받습니다.
    @Query(value = """
            select e from Ebook e
            where e.status in :statuses
              and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%'))
                                    or lower(e.author) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or e.categoryId = :categoryId)
            """,
            countQuery = """
            select count(e) from Ebook e
            where e.status in :statuses
              and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%'))
                                    or lower(e.author) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or e.categoryId = :categoryId)
            """)
    Page<Ebook> search(@Param("statuses") Collection<EbookStatus> statuses, @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId, Pageable pageable);

    // 챗봇 추천 후보 검색: 제목·저자에 소개 글까지 더해 찾습니다.
    // "번아웃", "위로"처럼 제목에는 없고 소개 글에만 있는 낱말로도 후보가 나오게 하려고 목록 검색과 따로 둡니다.
    @Query(value = """
            select e from Ebook e
            where e.status in :statuses
              and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%'))
                                    or lower(e.author) like lower(concat('%', :keyword, '%'))
                                    or lower(e.description) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or e.categoryId = :categoryId)
            """,
            countQuery = """
            select count(e) from Ebook e
            where e.status in :statuses
              and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%'))
                                    or lower(e.author) like lower(concat('%', :keyword, '%'))
                                    or lower(e.description) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or e.categoryId = :categoryId)
            """)
    Page<Ebook> searchIncludingDescription(@Param("statuses") Collection<EbookStatus> statuses,
            @Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);
}
