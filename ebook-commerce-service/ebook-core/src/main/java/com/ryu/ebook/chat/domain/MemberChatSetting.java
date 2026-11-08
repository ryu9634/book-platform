package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

// 계정별 챗봇 설정. 행이 없으면 ChatSetting의 기본값을 쓰므로 가입할 때 만들지 않고 관리자가 바꿀 때만 생깁니다.
@Getter
@Entity
@Table(name = "MEMBER_CHAT_SETTING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberChatSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MEMBER_CHAT_SETTING_SEQ_GENERATOR")
    @SequenceGenerator(name = "MEMBER_CHAT_SETTING_SEQ_GENERATOR", sequenceName = "MEMBER_CHAT_SETTING_SEQ",
            allocationSize = 50)
    @Column(name = "MEMBER_CHAT_SETTING_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "CHAT_ENABLED", nullable = false)
    private boolean chatEnabled;

    // NULL이면 전역 기본 한도를 따릅니다.
    private Long dailyTokenLimit;

    // 논리 참조: MEMBER
    private Long updatedBy;

    private MemberChatSetting(Long memberId, boolean chatEnabled, Long dailyTokenLimit, Long adminId) {
        validate(dailyTokenLimit);
        this.memberId = memberId;
        this.chatEnabled = chatEnabled;
        this.dailyTokenLimit = dailyTokenLimit;
        this.updatedBy = adminId;
    }

    public static MemberChatSetting of(Long memberId, boolean chatEnabled, Long dailyTokenLimit, Long adminId) {
        return new MemberChatSetting(memberId, chatEnabled, dailyTokenLimit, adminId);
    }

    public void change(boolean chatEnabled, Long dailyTokenLimit, Long adminId) {
        validate(dailyTokenLimit);
        this.chatEnabled = chatEnabled;
        this.dailyTokenLimit = dailyTokenLimit;
        this.updatedBy = adminId;
    }

    private static void validate(Long dailyTokenLimit) {
        if (dailyTokenLimit != null && dailyTokenLimit < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
