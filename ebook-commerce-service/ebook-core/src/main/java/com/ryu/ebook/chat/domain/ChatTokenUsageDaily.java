package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원별 일일 토큰 사용량. 날짜마다 행이 따로 생기므로 자정에 값을 초기화하는 배치가 필요 없습니다.
@Getter
@Entity
@Table(name = "CHAT_TOKEN_USAGE_DAILY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatTokenUsageDaily extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_TOKEN_USAGE_DAILY_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_TOKEN_USAGE_DAILY_SEQ_GENERATOR", sequenceName = "CHAT_TOKEN_USAGE_DAILY_SEQ",
            allocationSize = 50)
    @Column(name = "CHAT_TOKEN_USAGE_DAILY_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private long inputTokens;

    @Column(nullable = false)
    private long outputTokens;

    @Column(nullable = false)
    private long cacheCreateTokens;

    @Column(nullable = false)
    private long cacheReadTokens;

    @Column(nullable = false)
    private int requestCount;

    // 행을 만드는 것도 더하는 것도 ChatTokenUsageDailyRepository.addUsage(MERGE) 한 문장이 맡습니다.
    // 그래서 이 엔티티는 조회 전용이고 생성 팩토리를 두지 않습니다.
    public long totalTokens() {
        return inputTokens + outputTokens + cacheCreateTokens + cacheReadTokens;
    }
}
