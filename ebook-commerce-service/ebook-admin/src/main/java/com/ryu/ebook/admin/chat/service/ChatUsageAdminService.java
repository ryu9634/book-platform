package com.ryu.ebook.admin.chat.service;

import com.ryu.ebook.admin.chat.dto.AdminChatUsageResponse;
import com.ryu.ebook.admin.chat.dto.ChatSettingForm;
import com.ryu.ebook.admin.chat.dto.ChatUsageSummary;
import com.ryu.ebook.chat.config.ChatPricing;
import com.ryu.ebook.chat.domain.ChatSetting;
import com.ryu.ebook.chat.repository.ChatSettingRepository;
import com.ryu.ebook.chat.repository.ChatTokenUsageDailyRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 날짜별 토큰 사용량과 추정 비용, 그리고 전체 중단 스위치를 다룹니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatUsageAdminService {

    private final ChatTokenUsageDailyRepository chatTokenUsageDailyRepository;
    private final ChatSettingRepository chatSettingRepository;
    private final ChatPricing chatPricing;
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Page<AdminChatUsageResponse> usage(LocalDate date, Pageable pageable) {
        return chatTokenUsageDailyRepository.findViewsByDate(date, pageable)
                .map(view -> AdminChatUsageResponse.of(view, chatPricing));
    }

    public ChatUsageSummary summary(LocalDate date) {
        ChatSetting setting = setting();
        return ChatUsageSummary.of(chatTokenUsageDailyRepository.totalByDate(date), chatPricing,
                setting.getGlobalDailyTokenLimit());
    }

    public ChatSettingForm settingForm() {
        return ChatSettingForm.from(setting());
    }

    public boolean isEnabled() {
        return setting().isEnabled();
    }

    @Transactional
    public void changeSetting(boolean enabled, long defaultDailyTokenLimit, long globalDailyTokenLimit, Long adminId) {
        setting().change(enabled, defaultDailyTokenLimit, globalDailyTokenLimit);
        log.info("chat setting changed. enabled={}, defaultLimit={}, globalLimit={}, by={}", enabled,
                defaultDailyTokenLimit, globalDailyTokenLimit, adminId);
    }

    private ChatSetting setting() {
        return chatSettingRepository.findById(ChatSetting.SINGLE_ROW_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SETTING_NOT_FOUND));
    }
}
