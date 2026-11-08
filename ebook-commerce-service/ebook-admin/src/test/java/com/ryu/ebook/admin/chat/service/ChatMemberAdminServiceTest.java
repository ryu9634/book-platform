package com.ryu.ebook.admin.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.chat.domain.ChatSetting;
import com.ryu.ebook.chat.domain.MemberChatSetting;
import com.ryu.ebook.chat.repository.ChatSettingRepository;
import com.ryu.ebook.chat.repository.ChatTokenUsageDailyRepository;
import com.ryu.ebook.chat.repository.MemberChatSettingRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class ChatMemberAdminServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long ADMIN_ID = 9L;

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberChatSettingRepository memberChatSettingRepository = mock(MemberChatSettingRepository.class);
    private final ChatTokenUsageDailyRepository chatTokenUsageDailyRepository =
            mock(ChatTokenUsageDailyRepository.class);
    private final ChatSettingRepository chatSettingRepository = mock(ChatSettingRepository.class);
    private final ChatMemberAdminService service = new ChatMemberAdminService(memberRepository,
            memberChatSettingRepository, chatTokenUsageDailyRepository, chatSettingRepository,
            Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneId.of("Asia/Seoul")));

    private Member member() {
        Member member = MemberFixture.activeMember("reader01", "reader01@example.com");
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    // 전역 설정 행은 마이그레이션이 넣어 두고 코드가 만들지 않으므로, 단위 테스트에서는 대역으로 세웁니다.
    // 다른 스텁 호출 안에서 만들면 Mockito가 스텁을 끝내지 못하므로 미리 만들어 둡니다.
    private ChatSetting settingWithDefaultLimit(long defaultLimit) {
        ChatSetting setting = mock(ChatSetting.class);
        given(setting.getDefaultDailyTokenLimit()).willReturn(defaultLimit);
        return setting;
    }

    @Test
    @DisplayName("설정 행이 없는 회원은 사용 가능으로 보이고 적용 한도는 전역 기본값이 된다")
    void search_설정없음_기본값() {
        // given
        Page<Member> members = new PageImpl<>(List.of(member()), PageRequest.of(0, 20), 1);
        given(memberRepository.search(null, PageRequest.of(0, 20))).willReturn(members);
        given(memberChatSettingRepository.findByMemberIdIn(anyList())).willReturn(List.of());
        given(chatTokenUsageDailyRepository.findByUsageDateAndMemberIdIn(any(), anyList())).willReturn(List.of());
        ChatSetting setting = settingWithDefaultLimit(20_000L);
        given(chatSettingRepository.findById(ChatSetting.SINGLE_ROW_ID)).willReturn(Optional.of(setting));

        // when
        var row = service.search(null, PageRequest.of(0, 20)).getContent().getFirst();

        // then
        assertThat(row.chatEnabled()).isTrue();
        assertThat(row.dailyTokenLimit()).isNull();
        assertThat(row.effectiveLimit()).isEqualTo(20_000);
        assertThat(row.usedToday()).isZero();
        assertThat(row.limitLabel()).isEqualTo("기본값");
    }

    @Test
    @DisplayName("설정이 없던 회원은 새 행을 만들고, 있던 회원은 그 행을 고친다")
    void changeSetting_없으면생성_있으면수정() {
        // given
        given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
        given(memberChatSettingRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());

        // when
        service.changeSetting(MEMBER_ID, false, 5_000L, ADMIN_ID);

        // then
        verify(memberChatSettingRepository).save(any(MemberChatSetting.class));

        // given: 이미 행이 있을 때
        MemberChatSetting existing = MemberChatSetting.of(MEMBER_ID, true, null, ADMIN_ID);
        given(memberChatSettingRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        // when
        service.changeSetting(MEMBER_ID, true, 1_000L, ADMIN_ID);

        // then
        assertThat(existing.getDailyTokenLimit()).isEqualTo(1_000L);
        assertThat(existing.isChatEnabled()).isTrue();
    }

    @Test
    @DisplayName("없는 회원의 설정은 저장하지 않는다(FK가 없으므로 서비스가 확인한다)")
    void changeSetting_없는회원_예외() {
        // given
        given(memberRepository.existsById(MEMBER_ID)).willReturn(false);

        // when, then
        assertThatThrownBy(() -> service.changeSetting(MEMBER_ID, true, null, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(memberChatSettingRepository, never()).save(any());
    }
}
