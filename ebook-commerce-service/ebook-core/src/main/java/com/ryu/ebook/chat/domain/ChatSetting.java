package com.ryu.ebook.chat.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

// 챗봇 전역 운영 설정. 유료 API를 쓰므로 비용이 튀었을 때 재배포 없이 끌 수 있어야 해서 DB에 둡니다.
// 행은 항상 하나이고 마이그레이션이 미리 넣어 둡니다. 코드는 수정만 합니다.
@Getter
@Entity
@Table(name = "CHAT_SETTING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSetting extends BaseTimeEntity {

    public static final Long SINGLE_ROW_ID = 1L;

    // 시퀀스를 쓰지 않습니다. 행이 하나뿐이라 ID를 1로 고정하고 DB가 CHECK로 강제합니다.
    @Id
    @Column(name = "CHAT_SETTING_ID")
    private Long id;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private long defaultDailyTokenLimit;

    @Column(nullable = false)
    private long globalDailyTokenLimit;

    public void change(boolean enabled, long defaultDailyTokenLimit, long globalDailyTokenLimit) {
        if (defaultDailyTokenLimit < 0 || globalDailyTokenLimit < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.enabled = enabled;
        this.defaultDailyTokenLimit = defaultDailyTokenLimit;
        this.globalDailyTokenLimit = globalDailyTokenLimit;
    }
}
