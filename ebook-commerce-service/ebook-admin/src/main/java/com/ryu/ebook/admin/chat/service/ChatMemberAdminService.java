package com.ryu.ebook.admin.chat.service;

import com.ryu.ebook.chat.domain.ChatSetting;
import com.ryu.ebook.chat.domain.ChatTokenUsageDaily;
import com.ryu.ebook.chat.domain.MemberChatSetting;
import com.ryu.ebook.chat.repository.ChatSettingRepository;
import com.ryu.ebook.chat.repository.ChatTokenUsageDailyRepository;
import com.ryu.ebook.chat.repository.MemberChatSettingRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 계정별 챗봇 사용 여부와 일일 한도를 설정합니다. 오늘 얼마나 썼는지 같이 보여 줘야 한도를 정할 수 있습니다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMemberAdminService {

    private final MemberRepository memberRepository;
    private final MemberChatSettingRepository memberChatSettingRepository;
    private final ChatTokenUsageDailyRepository chatTokenUsageDailyRepository;
    private final ChatSettingRepository chatSettingRepository;
    private final Clock clock;

    public Page<ChatMemberRow> search(String keyword, Pageable pageable) {
        Page<Member> members = memberRepository
                .search(keyword == null || keyword.isBlank() ? null : keyword.trim(), pageable);
        List<Long> memberIds = members.getContent().stream().map(Member::getId).toList();
        if (memberIds.isEmpty()) {
            return members.map(member -> ChatMemberRow.of(member, null, 0, defaultLimit()));
        }

        Map<Long, MemberChatSetting> settings = memberChatSettingRepository.findByMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(MemberChatSetting::getMemberId, Function.identity(), (a, b) -> a));
        Map<Long, Long> usedToday = chatTokenUsageDailyRepository
                .findByUsageDateAndMemberIdIn(LocalDate.now(clock), memberIds).stream()
                .collect(Collectors.toMap(ChatTokenUsageDaily::getMemberId, ChatTokenUsageDaily::totalTokens,
                        (a, b) -> a));
        long defaultLimit = defaultLimit();
        return members.map(member -> ChatMemberRow.of(member, settings.get(member.getId()),
                usedToday.getOrDefault(member.getId(), 0L), defaultLimit));
    }

    // 설정 행이 없던 회원은 이때 만들어집니다. 기본값과 같은 값으로 저장해도 행이 생기지만,
    // 기본 한도를 나중에 바꿀 때 이 회원은 따라가지 않는다는 뜻이라 관리자가 정한 값을 그대로 둡니다.
    @Transactional
    public void changeSetting(Long memberId, boolean chatEnabled, Long dailyTokenLimit, Long adminId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Optional<MemberChatSetting> existing = memberChatSettingRepository.findByMemberId(memberId);
        if (existing.isPresent()) {
            existing.get().change(chatEnabled, dailyTokenLimit, adminId);
        } else {
            memberChatSettingRepository.save(MemberChatSetting.of(memberId, chatEnabled, dailyTokenLimit, adminId));
        }
        log.info("member chat setting changed. memberId={}, enabled={}, limit={}, by={}", memberId, chatEnabled,
                dailyTokenLimit, adminId);
    }

    private long defaultLimit() {
        return chatSettingRepository.findById(ChatSetting.SINGLE_ROW_ID)
                .map(ChatSetting::getDefaultDailyTokenLimit)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SETTING_NOT_FOUND));
    }

    // 화면에 필요한 값만 담습니다. dailyTokenLimit이 null이면 기본 한도를 따르는 회원입니다.
    public record ChatMemberRow(Long memberId, String loginId, String name, boolean active, boolean chatEnabled,
            Long dailyTokenLimit, long effectiveLimit, long usedToday) {

        static ChatMemberRow of(Member member, MemberChatSetting setting, long usedToday, long defaultLimit) {
            boolean enabled = setting == null || setting.isChatEnabled();
            Long limit = setting == null ? null : setting.getDailyTokenLimit();
            return new ChatMemberRow(member.getId(), member.getLoginId(), member.getName(), !member.isWithdrawn(),
                    enabled, limit, limit == null ? defaultLimit : limit, usedToday);
        }

        public String limitLabel() {
            return dailyTokenLimit == null ? "기본값" : String.valueOf(dailyTokenLimit);
        }
    }
}
