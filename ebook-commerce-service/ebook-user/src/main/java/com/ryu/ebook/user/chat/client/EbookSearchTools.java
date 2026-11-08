package com.ryu.ebook.user.chat.client;

import com.ryu.ebook.user.bookshelf.service.BookshelfQueryService;
import com.ryu.ebook.user.chat.config.ChatProperties;
import com.ryu.ebook.user.ebook.dto.CategoryOption;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.ebook.service.EbookQueryService;
import com.ryu.ebook.bookshelf.repository.BookshelfView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

// 모델에 열어 주는 도구. 추천할 책을 모델의 기억이 아니라 우리 DB에서 찾게 해서 없는 책을 지어내지 못하게 합니다.
// 도구는 판매 중인 전자책만 돌려주므로 숨긴 책과 판매 중지 책은 추천에 나오지 않습니다.
@Slf4j
@Component
@RequiredArgsConstructor
public class EbookSearchTools {

    private final EbookQueryService ebookQueryService;
    private final BookshelfQueryService bookshelfQueryService;
    private final ChatProperties chatProperties;

    @Tool(description = """
            판매 중인 전자책을 검색한다. 추천할 책은 반드시 이 도구로 찾고, 결과에 없는 책은 추천하지 않는다.
            검색어는 제목, 저자, 소개 글에서 찾는다. 한 질문에 쓸 수 있는 검색 횟수는 제한되어 있다.""")
    public EbookSearchResult searchEbooks(
            @ToolParam(description = "검색어. 책 제목, 저자 이름, 또는 분야를 나타내는 낱말 하나나 둘") String keyword,
            @ToolParam(description = "분류 번호. 특정 분류로 좁힐 때만 쓰고 모르면 비워 둔다", required = false)
            Long categoryId,
            ToolContext toolContext) {
        ChatToolSession session = ChatToolSession.from(toolContext);
        if (!session.consumeSearch()) {
            return EbookSearchResult.limitReached();
        }
        List<EbookSummaryResponse> books =
                ebookQueryService.searchForRecommendation(keyword, categoryId, chatProperties.candidateSize());
        session.addShown(books);
        log.info("chat tool searched ebooks. memberId={}, found={}", session.memberId(), books.size());
        return EbookSearchResult.of(books);
    }

    @Tool(description = "전자책 분류 목록을 가져온다. 검색을 특정 분류로 좁히고 싶을 때 먼저 부른다.")
    public List<CategoryOption> listCategories() {
        return ebookQueryService.categoryOptions();
    }

    @Tool(description = """
            질문한 회원이 이미 가진 전자책의 제목 목록을 가져온다.
            이미 가진 책을 추천하지 않으려면 이 도구를 먼저 부른다.""")
    public List<String> myOwnedBookTitles(ToolContext toolContext) {
        ChatToolSession session = ChatToolSession.from(toolContext);
        return bookshelfQueryService.myBookshelf(session.memberId()).stream()
                .map(BookshelfView::title)
                .toList();
    }
}
