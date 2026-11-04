package com.ryu.ebook.user.ebook.service;

import com.ryu.ebook.category.domain.Category;
import com.ryu.ebook.category.repository.CategoryRepository;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.ebook.dto.CategoryOption;
import com.ryu.ebook.user.ebook.dto.EbookDetailResponse;
import com.ryu.ebook.user.ebook.dto.EbookSort;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EbookQueryService {

    // 회원에게는 숨긴 전자책을 보여 주지 않습니다. 판매 중지 도서는 보이되 구매할 수 없습니다.
    private static final Set<EbookStatus> VISIBLE = EnumSet.of(EbookStatus.ON_SALE, EbookStatus.STOP_SALE);

    private final EbookRepository ebookRepository;
    private final CategoryRepository categoryRepository;

    public Page<EbookSummaryResponse> search(String keyword, Long categoryId, EbookSort sort, Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                (sort == null ? EbookSort.POPULAR : sort).sort());
        Map<Long, String> categoryNames = categoryNames();
        return ebookRepository.search(VISIBLE, normalized, categoryId, sorted)
                .map(ebook -> EbookSummaryResponse.of(ebook, categoryNames.get(ebook.getCategoryId())));
    }

    public List<EbookSummaryResponse> bestsellers() {
        return toSummaries(ebookRepository.findTop8ByStatusOrderBySalesCountDescIdDesc(EbookStatus.ON_SALE));
    }

    public List<EbookSummaryResponse> newArrivals() {
        return toSummaries(ebookRepository.findTop8ByStatusOrderByIdDesc(EbookStatus.ON_SALE));
    }

    // 숨긴 전자책은 없는 것과 똑같이 404로 답합니다.
    public EbookDetailResponse getEbook(Long ebookId) {
        Ebook ebook = ebookRepository.findById(ebookId)
                .filter(e -> !e.isHidden())
                .orElseThrow(() -> new BusinessException(ErrorCode.EBOOK_NOT_FOUND));
        return EbookDetailResponse.of(ebook, categoryNames().get(ebook.getCategoryId()));
    }

    public List<CategoryOption> categoryOptions() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(CategoryOption::from).toList();
    }

    private List<EbookSummaryResponse> toSummaries(List<Ebook> ebooks) {
        Map<Long, String> categoryNames = categoryNames();
        return ebooks.stream().map(e -> EbookSummaryResponse.of(e, categoryNames.get(e.getCategoryId()))).toList();
    }

    private Map<Long, String> categoryNames() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
