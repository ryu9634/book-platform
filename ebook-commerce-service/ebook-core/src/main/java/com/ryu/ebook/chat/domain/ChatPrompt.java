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
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

// 챗봇 시스템 프롬프트. 관리자가 고치면 이전 본문을 ChatPromptHistory에 남깁니다.
@Getter
@Entity
@Table(name = "CHAT_PROMPT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatPrompt extends BaseTimeEntity {

    // 도서 추천 대화에서 쓰는 프롬프트 키. 용도가 늘면 키를 추가합니다.
    public static final String BOOK_RECOMMEND = "BOOK_RECOMMEND";

    // 프롬프트는 매 요청의 입력 토큰으로 나가므로 길이를 제한합니다.
    private static final int MAX_CONTENT_LENGTH = 10_000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_PROMPT_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHAT_PROMPT_SEQ_GENERATOR", sequenceName = "CHAT_PROMPT_SEQ", allocationSize = 50)
    @Column(name = "CHAT_PROMPT_ID")
    private Long id;

    @Column(nullable = false, length = 50)
    private String promptKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    @Column(nullable = false)
    private String content;

    // 컬럼은 NUMBER(1)(0/1)입니다. Hibernate 7은 Oracle 23에서 boolean을 BOOLEAN 타입으로 기대하므로 숫자로 바꿔 저장합니다.
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    // 논리 참조: MEMBER. 마지막으로 고친 관리자입니다.
    private Long updatedBy;

    public void changeContent(String content, Long adminId) {
        validate(content);
        this.content = content.trim();
        this.updatedBy = adminId;
    }

    public void changeEnabled(boolean enabled, Long adminId) {
        this.enabled = enabled;
        this.updatedBy = adminId;
    }

    // DB에는 길이 제약이 없지만(CLOB), 빈 프롬프트로 바꾸면 모델이 아무 규칙 없이 답하게 되므로 막습니다.
    private static void validate(String content) {
        if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
