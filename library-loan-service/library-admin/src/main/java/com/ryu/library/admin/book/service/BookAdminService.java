package com.ryu.library.admin.book.service;

import com.ryu.library.admin.book.client.NationalLibraryClient;
import com.ryu.library.admin.book.dto.BookCopyResponse;
import com.ryu.library.admin.book.dto.BookForm;
import com.ryu.library.admin.book.dto.CopyStatusAction;
import com.ryu.library.admin.book.dto.BookResponse;
import com.ryu.library.admin.book.dto.CategoryOption;
import com.ryu.library.book.domain.Book;
import com.ryu.library.book.domain.BookCopy;
import com.ryu.library.book.repository.BookCopyRepository;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.domain.Category;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookAdminService {

    private static final int MAX_COPIES_PER_REQUEST = 10;

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CategoryRepository categoryRepository;
    private final NationalLibraryClient nationalLibraryClient;

    // 외부 API 호출이라 트랜잭션 밖에서 실행합니다(클래스의 readOnly 트랜잭션도 걸지 않음).
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookForm lookupByIsbn(String isbn) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN);
        }
        return nationalLibraryClient.findByIsbn(isbn)
                .map(BookForm::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ISBN_NOT_FOUND_IN_NL));
    }

    // 동시에 같은 ISBN을 등록하면 UNIQUE 제약이 막고 409로 바뀝니다.
    @Transactional
    public Long register(BookForm form) {
        if (bookRepository.existsByIsbn(form.getIsbn())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN);
        }
        validateCategory(form.getCategoryId());
        Book book = Book.builder()
                .categoryId(form.getCategoryId())
                .isbn(form.getIsbn())
                .title(form.getTitle())
                .author(form.getAuthor())
                .publisher(form.getPublisher())
                .publishedDate(form.getPublishedDate())
                .description(form.getDescription())
                .coverImageUrl(form.getCoverImageUrl())
                .build();
        Long bookId = bookRepository.save(book).getId();
        log.info("book registered. bookId={}", bookId);
        return bookId;
    }

    @Transactional
    public void update(Long bookId, BookForm form) {
        validateCategory(form.getCategoryId());
        Book book = findBook(bookId);
        book.update(form.getCategoryId(), form.getTitle(), form.getAuthor(), form.getPublisher(),
                form.getPublishedDate(), form.getDescription(), form.getCoverImageUrl());
        log.info("book updated. bookId={}", bookId);
    }

    // 권 번호는 이 도서에 등록된 권 수(폐기 포함) 다음 번호입니다. 동시에 추가해 바코드가 겹치면 UNIQUE 제약이 막습니다.
    @Transactional
    public int addCopies(Long bookId, int quantity) {
        if (quantity < 1 || quantity > MAX_COPIES_PER_REQUEST) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Book book = findBook(bookId);
        long next = bookCopyRepository.countByBookId(bookId) + 1;
        for (int i = 0; i < quantity; i++) {
            bookCopyRepository.save(BookCopy.create(book, BookCopy.barcodeOf(bookId, next + i)));
        }
        log.info("book copies added. bookId={}, quantity={}", bookId, quantity);
        return quantity;
    }

    @Transactional
    public Long changeCopyStatus(Long copyId, CopyStatusAction action) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));
        switch (action) {
            case LOST -> copy.markLost();
            case FOUND -> copy.restoreFound();
            case DISCARD -> copy.discard();
        }
        log.info("book copy status changed. copyId={}, action={}", copyId, action);
        return copy.getBook().getId();
    }

    public List<BookCopyResponse> getCopies(Long bookId) {
        return bookCopyRepository.findByBookIdOrderByIdAsc(bookId).stream().map(BookCopyResponse::from).toList();
    }

    // 분류 이름은 도서마다 따로 조회하지 않고(N+1 방지) 분류 전체를 한 번 읽어 맞춥니다. 분류는 몇 개 안 됩니다.
    public Page<BookResponse> findBooks(String keyword, Pageable pageable) {
        Page<Book> books = keyword == null || keyword.isBlank()
                ? bookRepository.findAll(pageable)
                : bookRepository.findByTitleContaining(keyword.trim(), pageable);
        Map<Long, String> categoryNames = categoryNames();
        return books.map(book -> BookResponse.of(book, categoryNames.get(book.getCategoryId())));
    }

    public BookResponse getBook(Long bookId) {
        Book book = findBook(bookId);
        return BookResponse.of(book, categoryNames().get(book.getCategoryId()));
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
    }

    // FK가 없으므로 존재하는 분류인지 서비스에서 확인합니다.
    private void validateCategory(Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    public List<CategoryOption> categoryOptions() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryOption::from).toList();
    }

    private Map<Long, String> categoryNames() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
