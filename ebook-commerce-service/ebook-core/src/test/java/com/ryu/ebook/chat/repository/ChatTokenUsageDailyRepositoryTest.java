package com.ryu.ebook.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ChatTokenUsageDailyRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Autowired
    ChatTokenUsageDailyRepository usageRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("사용량은 첫 호출에 행을 만들고 다음 호출부터 더하므로, 같은 날 여러 번 물어도 값이 덮어쓰이지 않는다")
    void addUsage_두번호출_누적() {
        // given
        Member member = memberRepository.save(MemberFixture.activeMember("cb01", "cb01@example.com"));

        // when
        usageRepository.addUsage(member.getId(), TODAY, 100, 40, 10, 20);
        usageRepository.addUsage(member.getId(), TODAY, 200, 60, 0, 30);

        // then
        var usage = usageRepository.findByMemberIdAndUsageDate(member.getId(), TODAY).orElseThrow();
        assertThat(usage.getInputTokens()).isEqualTo(300);
        assertThat(usage.getOutputTokens()).isEqualTo(100);
        assertThat(usage.getCacheCreateTokens()).isEqualTo(10);
        assertThat(usage.getCacheReadTokens()).isEqualTo(50);
        assertThat(usage.getRequestCount()).isEqualTo(2);
        assertThat(usage.totalTokens()).isEqualTo(460);
    }

    @Test
    @DisplayName("날짜가 다르면 행이 따로 쌓여서 자정이 지나면 한도가 저절로 초기화된다")
    void addUsage_다른날짜_행분리() {
        // given
        Member member = memberRepository.save(MemberFixture.activeMember("cb02", "cb02@example.com"));

        // when
        usageRepository.addUsage(member.getId(), TODAY, 100, 50, 0, 0);
        usageRepository.addUsage(member.getId(), TODAY.plusDays(1), 70, 30, 0, 0);

        // then
        assertThat(usageRepository.findByMemberIdAndUsageDate(member.getId(), TODAY).orElseThrow().totalTokens())
                .isEqualTo(150);
        assertThat(usageRepository.sumTotalTokensByDate(TODAY)).isEqualTo(150);
        assertThat(usageRepository.sumTotalTokensByDate(TODAY.plusDays(1))).isEqualTo(100);
        assertThat(usageRepository.sumTotalTokensByDate(TODAY.plusDays(2))).isZero();
    }

    @Test
    @DisplayName("관리자 사용량 목록은 회원 이름을 조인해 많이 쓴 순서로 주고, 합계도 한 번에 계산한다")
    void findViewsByDate_정렬_합계() {
        // given
        Member light = memberRepository.save(MemberFixture.activeMember("cb03", "cb03@example.com"));
        Member heavy = memberRepository.save(MemberFixture.activeMember("cb04", "cb04@example.com"));
        usageRepository.addUsage(light.getId(), TODAY, 100, 50, 0, 0);
        usageRepository.addUsage(heavy.getId(), TODAY, 1000, 500, 0, 0);

        // when
        var page = usageRepository.findViewsByDate(TODAY, PageRequest.of(0, 10));
        ChatUsageTotal total = usageRepository.totalByDate(TODAY);

        // then
        assertThat(page.getContent()).extracting(ChatUsageView::loginId).containsExactly("cb04", "cb03");
        assertThat(page.getContent()).extracting(ChatUsageView::memberName).containsOnly("홍길동");
        assertThat(page.getContent().getFirst().totalTokens()).isEqualTo(1500);
        assertThat(total.totalTokens()).isEqualTo(1650);
        assertThat(total.requests()).isEqualTo(2);
        assertThat(usageRepository.totalByDate(TODAY.plusDays(5)).totalTokens()).isZero();
    }
}
