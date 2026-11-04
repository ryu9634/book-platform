package com.ryu.ebook.admin.ebook.service;

import com.ryu.ebook.admin.ebook.dto.AdminEbookResponse;
import com.ryu.ebook.admin.ebook.dto.CategoryOption;
import com.ryu.ebook.admin.ebook.dto.EbookForm;
import com.ryu.ebook.category.domain.Category;
import com.ryu.ebook.category.repository.CategoryRepository;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.ebook.storage.EbookFileStorage;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EbookAdminService {

    private final EbookRepository ebookRepository;
    private final CategoryRepository categoryRepository;
    private final EbookFileStorage fileStorage;

    public Page<AdminEbookResponse> search(EbookStatus status, String keyword, Pageable pageable) {
        var statuses = status == null ? EnumSet.allOf(EbookStatus.class) : EnumSet.of(status);
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Map<Long, String> categoryNames = categoryNames();
        return ebookRepository.search(statuses, normalized, null, pageable)
                .map(ebook -> AdminEbookResponse.of(ebook, categoryNames.get(ebook.getCategoryId())));
    }

    public AdminEbookResponse get(Long ebookId) {
        Ebook ebook = find(ebookId);
        return AdminEbookResponse.of(ebook, categoryNames().get(ebook.getCategoryId()));
    }

    public EbookForm editForm(Long ebookId) {
        return EbookForm.from(find(ebookId));
    }

    // 같은 ISBN을 동시에 등록하면 UNIQUE 제약이 막고 409로 바뀝니다.
    @Transactional
    public Long register(EbookForm form) {
        String isbn = form.normalizedIsbn();
        if (isbn != null && ebookRepository.existsByIsbn(isbn)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN);
        }
        validateCategory(form.getCategoryId());
        Ebook ebook = Ebook.builder()
                .categoryId(form.getCategoryId()).isbn(isbn).title(form.getTitle()).author(form.getAuthor())
                .publisher(form.getPublisher()).description(form.getDescription())
                .coverImageUrl(form.getCoverImageUrl()).price(form.getPrice()).salePrice(form.getSalePrice())
                .publishedDate(form.getPublishedDate())
                .build();
        Long id = ebookRepository.save(ebook).getId();
        log.info("ebook registered. ebookId={}", id);
        return id;
    }

    @Transactional
    public void update(Long ebookId, EbookForm form) {
        validateCategory(form.getCategoryId());
        find(ebookId).update(form.getCategoryId(), form.getTitle(), form.getAuthor(), form.getPublisher(),
                form.getDescription(), form.getCoverImageUrl(), form.getPrice(), form.getSalePrice(),
                form.getPublishedDate());
        log.info("ebook updated. ebookId={}", ebookId);
    }

    @Transactional
    public void changeStatus(Long ebookId, EbookStatus status) {
        find(ebookId).changeStatus(status);
        log.info("ebook status changed. ebookId={}, status={}", ebookId, status);
    }

    // 파일을 먼저 디스크에 저장한 뒤 키를 기록합니다. 기록이 실패하면 디스크에 남은 파일은 참조되지 않는 파일로 남습니다.
    @Transactional
    public void uploadFile(Long ebookId, String originalFilename, long size, InputStream content) {
        Ebook ebook = find(ebookId);
        EbookFileStorage.StoredFile stored = fileStorage.store(originalFilename, size, content);
        ebook.attachFile(stored.fileKey(), stored.size(), stored.pageCount());
    }

    public List<CategoryOption> categoryOptions() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryOption::from).toList();
    }

    private Ebook find(Long ebookId) {
        return ebookRepository.findById(ebookId).orElseThrow(() -> new BusinessException(ErrorCode.EBOOK_NOT_FOUND));
    }

    // FK가 없으므로 존재하는 분류인지 서비스에서 확인합니다.
    private void validateCategory(Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private Map<Long, String> categoryNames() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
