package com.ryu.ebook.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.chat.domain.ChatSetting;
import com.ryu.ebook.chat.domain.MemberChatSetting;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.chat.repository.ChatSettingRepository;
import com.ryu.ebook.chat.repository.ChatTokenUsageDailyRepository;
import com.ryu.ebook.chat.repository.MemberChatSettingRepository;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

// 전역 설정 행은 마이그레이션이 넣어 두므로 실제 기본값으로 검증합니다. 날짜가 결과를 바꾸니 시계를 고정합니다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ChatQuotaServiceTest {

    private static final Long MEMBER_ID = 1_001L;
    private static final Long ADMIN_ID = 99L;

    @Autowired
    ChatSettingRepository chatSettingRepository;

    @Autowired
    MemberChatSettingRepository memberChatSettingRepository;

    @Autowired
    ChatTokenUsageDailyRepository chatTokenUsageDailyRepository;

    ChatQuotaService chatQuotaService;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        chatQuotaService = new ChatQuotaService(chatSettingRepository, memberChatSettingRepository,
                chatTokenUsageDailyRepository, fixed);
    }

    @Test
    @DisplayName("계정별 설정이 없으면 전역 기본 한도를 그대로 쓰고, 쓴 토큰은 더해져 남은 양이 줄어든다")
    void quota_설정없는회원_기본한도() {
        // given
        ChatSetting setting = chatQuotaService.setting();
        assertThat(setting.getDefaultDailyTokenLimit()).isEqualTo(20_000);

        // when
        chatQuotaService.record(MEMBER_ID, new TokenUsage(1_000, 200, 0, 100));
        chatQuotaService.record(MEMBER_ID, TokenUsage.NONE);
        ChatQuota quota = chatQuotaService.quota(MEMBER_ID);

        // then
        assertThat(quota.dailyTokenLimit()).isEqualTo(20_000);
        assertThat(quota.usedTokens()).isEqualTo(1_300);
        assertThat(quota.remainingTokens()).isEqualTo(18_700);
        assertThat(quota.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("계정 한도를 다 쓰면 429로 막고, 계정별로 한도를 낮추면 그 값이 먼저 적용된다")
    void ensureAvailable_한도초과_예외() {
        // given
        memberChatSettingRepository.save(MemberChatSetting.of(MEMBER_ID, true, 1_000L, ADMIN_ID));
        chatQuotaService.record(MEMBER_ID, TokenUsage.of(900, 100));

        // when, then
        assertThat(chatQuotaService.quota(MEMBER_ID).dailyTokenLimit()).isEqualTo(1_000);
        assertThatThrownBy(() -> chatQuotaService.ensureAvailable(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("계정을 끄면 403, 서비스 전체를 끄면 503으로 막는다")
    void ensureAvailable_비활성_예외() {
        // given
        memberChatSettingRepository.saveAndFlush(MemberChatSetting.of(MEMBER_ID, false, null, ADMIN_ID));

        // when, then
        assertThatThrownBy(() -> chatQuotaService.ensureAvailable(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHATBOT_DISABLED);

        ChatSetting setting = chatQuotaService.setting();
        setting.change(false, setting.getDefaultDailyTokenLimit(), setting.getGlobalDailyTokenLimit());
        chatSettingRepository.saveAndFlush(setting);
        assertThatThrownBy(() -> chatQuotaService.ensureAvailable(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHATBOT_SUSPENDED);
    }

    @Test
    @DisplayName("서비스 전체 일일 상한을 넘으면 개별 한도가 남아 있어도 전체가 중단된다")
    void ensureAvailable_전역상한초과_중단() {
        // given
        ChatSetting setting = chatQuotaService.setting();
        setting.change(true, setting.getDefaultDailyTokenLimit(), 1_000);
        chatSettingRepository.saveAndFlush(setting);
        chatQuotaService.record(2_002L, TokenUsage.of(900, 100));

        // when, then
        ChatQuota quota = chatQuotaService.quota(MEMBER_ID);
        assertThat(quota.remainingTokens()).isEqualTo(20_000);
        assertThat(quota.isGlobalLimitReached()).isTrue();
        assertThatThrownBy(() -> chatQuotaService.ensureAvailable(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHATBOT_SUSPENDED);
    }
}
