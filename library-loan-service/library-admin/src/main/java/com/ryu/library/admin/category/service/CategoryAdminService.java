package com.ryu.library.admin.category.service;

import com.ryu.library.admin.category.dto.CategoryNode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    // 분류 전체를 한 번 읽어 메모리에서 트리를 만듭니다. 상위 분류를 지연 로딩하지 않도록 ID로만 묶습니다.
    public List<CategoryNode> tree() {
        List<Category> all = categoryRepository.findAllByOrderBySortOrderAsc();
        Map<Long, List<Category>> childrenByParent = all.stream()
                .filter(category -> !category.isTopLevel())
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));
        return all.stream()
                .filter(Category::isTopLevel)
                .map(top -> new CategoryNode(top.getId(), top.getName(), top.getSortOrder(),
                        childrenByParent.getOrDefault(top.getId(), List.of()).stream()
                                .map(child -> new CategoryNode(child.getId(), child.getName(), child.getSortOrder(), List.of()))
                                .toList()))
                .toList();
    }

    @Transactional
    public Long create(Long parentId, String name, int sortOrder) {
        Category parent = parentId == null ? null : find(parentId);
        Category saved = categoryRepository.save(Category.create(parent, validName(name), sortOrder));
        log.info("category created. categoryId={}, parentId={}", saved.getId(), parentId);
        return saved.getId();
    }

    @Transactional
    public void rename(Long categoryId, String name, int sortOrder) {
        find(categoryId).rename(validName(name), sortOrder);
    }

    // FK가 없으므로 하위 분류나 소속 도서가 있는지 서비스에서 먼저 확인한 뒤에만 지웁니다.
    @Transactional
    public void delete(Long categoryId) {
        Category category = find(categoryId);
        if (categoryRepository.existsByParentId(categoryId) || bookRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
        log.info("category deleted. categoryId={}", categoryId);
    }

    private Category find(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private static String validName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return name.trim();
    }
}
