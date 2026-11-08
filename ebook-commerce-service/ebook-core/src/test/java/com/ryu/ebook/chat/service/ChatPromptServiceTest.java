package com.ryu.ebook.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.chat.domain.ChatPrompt;
import com.ryu.ebook.chat.repository.ChatPromptHistoryRepository;
import com.ryu.ebook.chat.repository.ChatPromptRepository;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

// 프롬프트 행은 코드가 만들지 않고 마이그레이션이 넣어 두므로, 엔티티를 가짜로 만들지 않고 실제 기본 데이터로 검증합니다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class ChatPromptServiceTest {

    private static final Long ADMIN_ID = 99L;

    @Autowired
    ChatPromptRepository chatPromptRepository;

    @Autowired
    ChatPromptHistoryRepository chatPromptHistoryRepository;

    ChatPromptService chatPromptService;

    @BeforeEach
    void setUp() {
        chatPromptService = new ChatPromptService(chatPromptRepository, chatPromptHistoryRepository);
    }

    @Test
    @DisplayName("기본 도서 추천 프롬프트가 켜진 상태로 들어 있고, 검색한 책만 추천하라는 규칙을 담고 있다")
    void activeContent_기본데이터_사용가능() {
        // given, when
        String content = chatPromptService.activeContent(ChatPrompt.BOOK_RECOMMEND);

        // then
        assertThat(content).contains("검색 도구").contains("추천하지 않습니다");
        assertThatThrownBy(() -> chatPromptService.activeContent("NO_SUCH_KEY"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_PROMPT_NOT_FOUND);
    }

    @Test
    @DisplayName("프롬프트를 고치면 이전 본문이 이력에 남고, 같은 내용으로 다시 저장하면 이력이 늘지 않는다")
    void changeContent_수정_이력남김() {
        // given
        ChatPrompt prompt = chatPromptRepository.findByPromptKey(ChatPrompt.BOOK_RECOMMEND).orElseThrow();
        String before = prompt.getContent();

        // when
        chatPromptService.changeContent(prompt.getId(), "새 프롬프트 본문", ADMIN_ID);
        chatPromptService.changeContent(prompt.getId(), "새 프롬프트 본문", ADMIN_ID);

        // then
        var histories = chatPromptHistoryRepository
                .findByChatPromptIdOrderByIdDesc(prompt.getId(), PageRequest.of(0, 10));
        assertThat(histories.getTotalElements()).isEqualTo(1);
        assertThat(histories.getContent().getFirst().getContent()).isEqualTo(before);
        assertThat(histories.getContent().getFirst().getChangedBy()).isEqualTo(ADMIN_ID);
        assertThat(chatPromptService.activeContent(ChatPrompt.BOOK_RECOMMEND)).isEqualTo("새 프롬프트 본문");
        assertThat(prompt.getUpdatedBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("빈 프롬프트로는 바꿀 수 없고, 꺼 두면 회원 앱이 답변을 만들지 못한다")
    void changeContent_빈값_예외() {
        // given
        ChatPrompt prompt = chatPromptRepository.findByPromptKey(ChatPrompt.BOOK_RECOMMEND).orElseThrow();

        // when, then
        assertThatThrownBy(() -> chatPromptService.changeContent(prompt.getId(), "  ", ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        chatPromptService.changeEnabled(prompt.getId(), false, ADMIN_ID);
        assertThatThrownBy(() -> chatPromptService.activeContent(ChatPrompt.BOOK_RECOMMEND))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHATBOT_SUSPENDED);
    }
}
