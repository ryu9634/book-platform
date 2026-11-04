package com.ryu.ebook.admin.ebook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ryu.ebook.admin.ebook.dto.EbookForm;
import com.ryu.ebook.category.repository.CategoryRepository;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.ebook.storage.EbookFileStorage;
import com.ryu.ebook.global.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EbookAdminServiceTest {

    private final EbookRepository ebookRepository = mock(EbookRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final EbookFileStorage fileStorage = mock(EbookFileStorage.class);
    private final EbookAdminService service = new EbookAdminService(ebookRepository, categoryRepository, fileStorage);

    private EbookForm form(long price, long salePrice) {
        EbookForm form = new EbookForm();
        form.setIsbn("9788966262281");
        form.setTitle("이펙티브 자바");
        form.setAuthor("조슈아 블로크");
        form.setPrice(price);
        form.setSalePrice(salePrice);
        return form;
    }

    @Test
    @DisplayName("판매 중 상태로 등록하고, 이미 있는 ISBN이나 정가보다 비싼 판매가는 거부한다")
    void register_검증() {
        given(ebookRepository.save(any(Ebook.class))).willAnswer(invocation -> invocation.getArgument(0));
        service.register(form(36000, 32400));

        given(ebookRepository.existsByIsbn("9788966262281")).willReturn(true);
        assertThatThrownBy(() -> service.register(form(36000, 32400)))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ISBN);

        given(ebookRepository.existsByIsbn("9788966262281")).willReturn(false);
        assertThatThrownBy(() -> service.register(form(10000, 20000)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PRICE);
    }

    @Test
    @DisplayName("본문 파일을 저장소에 저장하고 파일 키와 쪽수를 전자책에 기록한다")
    void uploadFile_기록() {
        // given
        Ebook ebook = EbookFixture.ebook("9780000000001", "제목", 10000, 9000);
        given(ebookRepository.findById(1L)).willReturn(Optional.of(ebook));
        given(fileStorage.store(anyString(), anyLong(), any())).willReturn(new EbookFileStorage.StoredFile("key.txt", 30, 2));

        // when
        service.uploadFile(1L, "본문.txt", 30, new ByteArrayInputStream(new byte[30]));

        // then
        assertThat(ebook.getFileKey()).isEqualTo("key.txt");
        assertThat(ebook.getPageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("판매 상태를 바꾸고, 없는 분류를 고르면 수정하지 않는다")
    void 상태변경_분류확인() {
        Ebook ebook = EbookFixture.ebook("9780000000001", "제목", 10000, 9000);
        given(ebookRepository.findById(1L)).willReturn(Optional.of(ebook));

        service.changeStatus(1L, EbookStatus.STOP_SALE);
        assertThat(ebook.isOnSale()).isFalse();

        EbookForm form = form(10000, 9000);
        form.setCategoryId(999L);
        assertThatThrownBy(() -> service.update(1L, form)).extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        verify(ebookRepository, never()).save(any());
    }
}
