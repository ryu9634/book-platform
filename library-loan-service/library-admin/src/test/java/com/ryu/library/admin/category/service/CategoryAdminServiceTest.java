package com.ryu.library.admin.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.library.admin.category.dto.CategoryNode;
import com.ryu.library.book.repository.BookRepository;
import com.ryu.library.category.domain.Category;
import com.ryu.library.category.repository.CategoryRepository;
import com.ryu.library.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryAdminServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final CategoryAdminService service = new CategoryAdminService(categoryRepository, bookRepository);

    private Category withId(Category category, long id) {
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    @DisplayName("분류 전체를 한 번 읽어 최상위 아래 하위 분류를 붙인 트리를 만든다")
    void tree_2단계() {
        Category top = withId(Category.create(null, "문학", 1), 1L);
        Category child = withId(Category.create(top, "한국소설", 1), 2L);
        given(categoryRepository.findAllByOrderBySortOrderAsc()).willReturn(List.of(top, child));

        List<CategoryNode> tree = service.tree();

        assertThat(tree).singleElement().satisfies(node -> {
            assertThat(node.name()).isEqualTo("문학");
            assertThat(node.children()).extracting(CategoryNode::name).containsExactly("한국소설");
        });
    }

    @Test
    @DisplayName("하위 분류나 도서가 있는 분류는 삭제하지 않는다 (FK가 없으므로 서비스에서 확인)")
    void delete_사용중_B011() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(withId(Category.create(null, "문학", 1), 1L)));
        given(bookRepository.existsByCategoryId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_IN_USE);
        verify(categoryRepository, never()).delete(any());
    }
}
