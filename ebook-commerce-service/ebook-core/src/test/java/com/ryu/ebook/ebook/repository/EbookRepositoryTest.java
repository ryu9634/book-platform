package com.ryu.ebook.ebook.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.support.TestcontainersConfiguration;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class EbookRepositoryTest {

    @Autowired
    EbookRepository ebookRepository;

    @Test
    @DisplayName("회원에게 보일 상태만 검색하고, 판매가 정렬과 검색어로 거른다")
    void search_상태_검색어_정렬() {
        // given
        ebookRepository.save(EbookFixture.ebook("9780000000011", "자바 입문", 20000, 18000));
        ebookRepository.save(EbookFixture.ebook("9780000000012", "모던 자바", 30000, 15000));
        Ebook hidden = EbookFixture.ebook("9780000000013", "숨긴 자바", 10000, 10000);
        hidden.changeStatus(EbookStatus.HIDDEN);
        ebookRepository.saveAndFlush(hidden);
        var visible = EnumSet.of(EbookStatus.ON_SALE, EbookStatus.STOP_SALE);

        // when
        var page = ebookRepository.search(visible, "자바", null, PageRequest.of(0, 10, Sort.by("salePrice")));

        // then
        assertThat(page.getContent()).extracting(Ebook::getTitle).containsExactly("모던 자바", "자바 입문");
        assertThat(ebookRepository.findTop8ByStatusOrderByIdDesc(EbookStatus.ON_SALE)).hasSize(2);
    }
}
