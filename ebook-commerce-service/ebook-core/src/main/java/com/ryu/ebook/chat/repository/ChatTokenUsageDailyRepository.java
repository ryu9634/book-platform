package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatTokenUsageDaily;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatTokenUsageDailyRepository extends JpaRepository<ChatTokenUsageDaily, Long> {

    Optional<ChatTokenUsageDaily> findByMemberIdAndUsageDate(Long memberId, LocalDate usageDate);

    // 관리자 회원 목록에서 오늘 사용량을 한 번에 가져옵니다(N+1 방지).
    List<ChatTokenUsageDaily> findByUsageDateAndMemberIdIn(LocalDate usageDate, Collection<Long> memberIds);

    // 같은 회원이 동시에 두 번 물어도 사용량이 덮어쓰이지 않도록 DB에서 더합니다.
    // 행이 없으면 만들고 있으면 더하는 것을 한 문장으로 처리해, 하루 첫 요청이 동시에 들어와도
    // UNIQUE 제약 충돌을 잡아 재시도하는 코드가 필요 없게 합니다. Oracle 전용 구문입니다.
    // CREATED_AT/UPDATED_AT은 감사 컬럼이라 JPA Auditing 대신 DB 시각을 씁니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            MERGE INTO CHAT_TOKEN_USAGE_DAILY t
            USING (SELECT :memberId AS MEMBER_ID, :usageDate AS USAGE_DATE FROM dual) s
               ON (t.MEMBER_ID = s.MEMBER_ID AND t.USAGE_DATE = s.USAGE_DATE)
            WHEN MATCHED THEN UPDATE SET
                 INPUT_TOKENS        = t.INPUT_TOKENS + :inputTokens,
                 OUTPUT_TOKENS       = t.OUTPUT_TOKENS + :outputTokens,
                 CACHE_CREATE_TOKENS = t.CACHE_CREATE_TOKENS + :cacheCreateTokens,
                 CACHE_READ_TOKENS   = t.CACHE_READ_TOKENS + :cacheReadTokens,
                 REQUEST_COUNT       = t.REQUEST_COUNT + 1,
                 UPDATED_AT          = SYSTIMESTAMP
            WHEN NOT MATCHED THEN INSERT (CHAT_TOKEN_USAGE_DAILY_ID, MEMBER_ID, USAGE_DATE, INPUT_TOKENS,
                 OUTPUT_TOKENS, CACHE_CREATE_TOKENS, CACHE_READ_TOKENS, REQUEST_COUNT, CREATED_AT, UPDATED_AT)
            VALUES (CHAT_TOKEN_USAGE_DAILY_SEQ.NEXTVAL, :memberId, :usageDate, :inputTokens, :outputTokens,
                 :cacheCreateTokens, :cacheReadTokens, 1, SYSTIMESTAMP, SYSTIMESTAMP)
            """)
    void addUsage(@Param("memberId") Long memberId, @Param("usageDate") LocalDate usageDate,
            @Param("inputTokens") long inputTokens, @Param("outputTokens") long outputTokens,
            @Param("cacheCreateTokens") long cacheCreateTokens, @Param("cacheReadTokens") long cacheReadTokens);

    // 서비스 전체 일일 상한을 확인할 때 씁니다.
    @Query("""
            select coalesce(sum(u.inputTokens + u.outputTokens + u.cacheCreateTokens + u.cacheReadTokens), 0)
            from ChatTokenUsageDaily u where u.usageDate = :usageDate
            """)
    long sumTotalTokensByDate(@Param("usageDate") LocalDate usageDate);

    @Query(value = """
            select new com.ryu.ebook.chat.repository.ChatUsageView(u.memberId, m.loginId, m.name,
                   u.inputTokens, u.outputTokens, u.cacheCreateTokens, u.cacheReadTokens, u.requestCount)
            from ChatTokenUsageDaily u join Member m on m.id = u.memberId
            where u.usageDate = :usageDate
            order by u.inputTokens + u.outputTokens + u.cacheCreateTokens + u.cacheReadTokens desc
            """,
            countQuery = "select count(u) from ChatTokenUsageDaily u where u.usageDate = :usageDate")
    Page<ChatUsageView> findViewsByDate(@Param("usageDate") LocalDate usageDate, Pageable pageable);

    @Query("""
            select new com.ryu.ebook.chat.repository.ChatUsageTotal(coalesce(sum(u.inputTokens), 0),
                   coalesce(sum(u.outputTokens), 0), coalesce(sum(u.cacheCreateTokens), 0),
                   coalesce(sum(u.cacheReadTokens), 0), coalesce(sum(u.requestCount), 0))
            from ChatTokenUsageDaily u where u.usageDate = :usageDate
            """)
    ChatUsageTotal totalByDate(@Param("usageDate") LocalDate usageDate);
}
