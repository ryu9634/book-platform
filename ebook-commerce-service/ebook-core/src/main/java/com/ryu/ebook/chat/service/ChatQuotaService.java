package com.ryu.ebook.chat.service;

import com.ryu.ebook.chat.domain.ChatQuota;
import com.ryu.ebook.chat.domain.ChatSetting;
import com.ryu.ebook.chat.domain.ChatTokenUsageDaily;
import com.ryu.ebook.chat.domain.MemberChatSetting;
import com.ryu.ebook.chat.domain.TokenUsage;
import com.ryu.ebook.chat.repository.ChatSettingRepository;
import com.ryu.ebook.chat.repository.ChatTokenUsageDailyRepository;
import com.ryu.ebook.chat.repository.MemberChatSettingRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 챗봇을 쓸 수 있는지 판단하고, 쓴 토큰을 기록합니다.
// 회원 앱은 호출 전 확인과 호출 후 기록에, 관리자 앱은 적용 중인 한도를 보여 줄 때 씁니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQuotaService {

    private final ChatSettingRepository chatSettingRepository;
    private final MemberChatSettingRepository memberChatSettingRepository;
    private final ChatTokenUsageDailyRepository chatTokenUsageDailyRepository;
    private final Clock clock;

    public ChatSetting setting() {
        return chatSettingRepository.findById(ChatSetting.SINGLE_ROW_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SETTING_NOT_FOUND));
    }

    // 계정별 설정 행이 없으면 전역 기본값을 씁니다. 그래서 가입할 때 설정 행을 만들어 두지 않아도 됩니다.
    public ChatQuota quota(Long memberId) {
        ChatSetting setting = setting();
        Optional<MemberChatSetting> memberSetting = memberChatSettingRepository.findByMemberId(memberId);
        boolean memberEnabled = memberSetting.map(MemberChatSetting::isChatEnabled).orElse(true);
        long limit = memberSetting.map(MemberChatSetting::getDailyTokenLimit)
                .orElse(setting.getDefaultDailyTokenLimit());

        LocalDate today = LocalDate.now(clock);
        long used = chatTokenUsageDailyRepository.findByMemberIdAndUsageDate(memberId, today)
                .map(ChatTokenUsageDaily::totalTokens)
                .orElse(0L);
        long globalUsed = chatTokenUsageDailyRepository.sumTotalTokensByDate(today);
        return new ChatQuota(setting.isEnabled(), memberEnabled, limit, used,
                setting.getGlobalDailyTokenLimit(), globalUsed);
    }

    // 모델을 부르기 전에 확인합니다. 한도는 호출 뒤에 기록하므로 마지막 한 번은 한도를 조금 넘길 수 있습니다.
    // 그 오차를 막으려고 매 요청에 락을 걸기보다, 답변 하나 분량은 넘칠 수 있다고 보고 넘어갑니다.
    public ChatQuota ensureAvailable(Long memberId) {
        ChatQuota quota = quota(memberId);
        if (!quota.serviceEnabled() || quota.isGlobalLimitReached()) {
            throw new BusinessException(ErrorCode.CHATBOT_SUSPENDED);
        }
        if (!quota.memberEnabled()) {
            throw new BusinessException(ErrorCode.CHATBOT_DISABLED);
        }
        if (quota.isMemberLimitReached()) {
            throw new BusinessException(ErrorCode.CHAT_DAILY_LIMIT_EXCEEDED);
        }
        return quota;
    }

    @Transactional
    public void record(Long memberId, TokenUsage usage) {
        if (usage == null || usage.totalTokens() <= 0) {
            return;
        }
        chatTokenUsageDailyRepository.addUsage(memberId, LocalDate.now(clock), usage.inputTokens(),
                usage.outputTokens(), usage.cacheCreateTokens(), usage.cacheReadTokens());
        log.info("chat token usage recorded. memberId={}, totalTokens={}", memberId, usage.totalTokens());
    }
}
