package com.ryu.library.user.book.service;

import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopyStatus;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.domain.Category;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.loan.repository.PopularBookView;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import com.ryu.library.user.book.dto.BookDetailResponse;
import com.ryu.library.user.book.dto.BookSummaryResponse;
import com.ryu.library.user.book.dto.CategoryOption;
import com.ryu.library.user.book.dto.CopyCounts;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookQueryService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    private static final int POPULAR_DAYS = 90;
    private static final int POPULAR_MAX = 20;

    // 도서 목록 1번 + 권 수 집계 1번 + 분류 1번. 도서 수만큼 쿼리가 늘지 않습니다(N+1 방지).
    public Page<BookSummaryResponse> search(String keyword, Long categoryId, Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Book> books = bookRepository.search(normalized, categoryId, pageable);
        return toSummaries(books);
    }

    public List<BookSummaryResponse> newArrivals() {
        List<Book> books = bookRepository.findTop8ByOrderByIdDesc();
        Map<Long, CopyCounts> counts = copyCounts(books);
        Map<Long, String> categoryNames = categoryNames();
        return books.stream()
                .map(book -> BookSummaryResponse.of(book, categoryNames.get(book.getCategoryId()),
                        counts.getOrDefault(book.getId(), CopyCounts.NONE)))
                .toList();
    }

    // 최근 90일 대출 수 순위. ebook 서비스가 "도서관 인기 도서를 전자책으로" 추천에 씁니다.
    public List<PopularBookView> popular(int limit) {
        int size = Math.min(Math.max(limit, 1), POPULAR_MAX);
        return loanRepository.findPopular(LocalDateTime.now(clock).minusDays(POPULAR_DAYS), PageRequest.of(0, size));
    }

    public BookDetailResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        return BookDetailResponse.of(book, categoryNames().get(book.getCategoryId()),
                bookCopyRepository.findByBookIdOrderByIdAsc(bookId));
    }

    public List<CategoryOption> categoryOptions() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryOption::from).toList();
    }

    private Page<BookSummaryResponse> toSummaries(Page<Book> books) {
        Map<Long, CopyCounts> counts = copyCounts(books.getContent());
        Map<Long, String> categoryNames = categoryNames();
        return books.map(book -> BookSummaryResponse.of(book, categoryNames.get(book.getCategoryId()),
                counts.getOrDefault(book.getId(), CopyCounts.NONE)));
    }

    private Map<Long, CopyCounts> copyCounts(List<Book> books) {
        if (books.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<BookCopyStatus, Long>> byBook = new HashMap<>();
        bookCopyRepository.countByBookIdsGroupByStatus(books.stream().map(Book::getId).toList())
                .forEach(row -> byBook.computeIfAbsent(row.getBookId(), id -> new EnumMap<>(BookCopyStatus.class))
                        .put(row.getStatus(), row.getCount()));
        return byBook.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> CopyCounts.from(entry.getValue())));
    }

    private Map<Long, String> categoryNames() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
