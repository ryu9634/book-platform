package com.ryu.ebook.user.ebook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.category.repository.CategoryRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.ebook.dto.EbookSort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class EbookQueryServiceTest {

    private final EbookRepository ebookRepository = mock(EbookRepository.class);
    private final EbookQueryService service = new EbookQueryService(ebookRepository, mock(CategoryRepository.class));

    @Test
    @DisplayName("정렬 값을 정해진 정렬로 바꿔 조회한다")
    void search_정렬변환() {
        given(ebookRepository.search(anyCollection(), any(), any(), any())).willReturn(Page.empty());

        service.search(null, null, EbookSort.PRICE_ASC, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ebookRepository).search(anyCollection(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("salePrice").isAscending()).isTrue();
    }

    @Test
    @DisplayName("숨긴 전자책 상세는 없는 것과 똑같이 E001로 답한다")
    void getEbook_숨김_E001() {
        Ebook hidden = EbookFixture.ebook("9780000000001", "제목", 10000, 9000);
        hidden.changeStatus(EbookStatus.HIDDEN);
        given(ebookRepository.findById(1L)).willReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.getEbook(1L)).extracting("errorCode").isEqualTo(ErrorCode.EBOOK_NOT_FOUND);
    }
}
