package com.ryu.ebook.user.chat.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ryu.ebook.bookshelf.repository.BookshelfView;
import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class EbookSearchToolsTest {

    private static final Long MEMBER_ID = 5L;

    private final EbookQueryService ebookQueryService = mock(EbookQueryService.class);
    private final BookshelfQueryService bookshelfQueryService = mock(BookshelfQueryService.class);
    private final ChatProperties chatProperties =
            new ChatProperties("claude-haiku-5-5", "low", 1200, 10, 2, 10, Duration.ofSeconds(3));
    private final EbookSearchTools tools =
            new EbookSearchTools(ebookQueryService, bookshelfQueryService, chatProperties);

    private ToolContext toolContext(ChatToolSession session) {
        return new ToolContext(Map.of(ChatToolSession.KEY, session));
    }

    @Test
    @DisplayName("검색 횟수를 다 쓰면 더 검색하지 않고 안내만 돌려주므로 한 질문이 토큰을 무한히 쓰지 못한다")
    void searchEbooks_횟수초과_안내() {
        // given
        ChatToolSession session = new ChatToolSession(MEMBER_ID, chatProperties.maxToolCalls());
        ToolContext context = toolContext(session);
        given(ebookQueryService.searchForRecommendation(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of(new EbookSummaryResponse(1L, "이펙티브 자바", "조슈아 블로크", null, 36000, 32400, 10,
                        "IT/프로그래밍", true)));

        // when
        for (int i = 0; i < chatProperties.maxToolCalls(); i++) {
            assertThat(tools.searchEbooks("자바", null, context).books()).hasSize(1);
        }
        EbookSearchResult exceeded = tools.searchEbooks("자바", null, context);

        // then
        assertThat(exceeded.books()).isEmpty();
        assertThat(exceeded.notice()).contains("검색 횟수를 모두 썼다");
        assertThat(session.shownBooks()).hasSize(chatProperties.maxToolCalls());
    }

    @Test
    @DisplayName("검색 결과가 없으면 지어내지 말라는 안내를 함께 돌려준다")
    void searchEbooks_결과없음_안내() {
        // given
        ToolContext context = toolContext(new ChatToolSession(MEMBER_ID, 3));
        given(ebookQueryService.searchForRecommendation(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of());

        // when
        EbookSearchResult result = tools.searchEbooks("없는 책", null, context);

        // then
        assertThat(result.books()).isEmpty();
        assertThat(result.notice()).contains("검색 결과가 없다");
    }

    @Test
    @DisplayName("보유 도서 조회는 도구 인자가 아니라 ToolContext의 회원 번호로 하므로 남의 서재를 볼 수 없다")
    void myOwnedBookTitles_컨텍스트회원() {
        // given
        ToolContext context = toolContext(new ChatToolSession(MEMBER_ID, 3));
        given(bookshelfQueryService.myBookshelf(MEMBER_ID)).willReturn(List.of(
                new BookshelfView(1L, 10L, "클린 코드", "로버트 마틴", null, 500, 0, null, LocalDateTime.now())));

        // when
        List<String> titles = tools.myOwnedBookTitles(context);

        // then
        assertThat(titles).containsExactly("클린 코드");
    }
}
