package com.ryu.library.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.library.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    @DisplayName("분류는 2단계까지 만들고, 하위 분류 아래에는 만들 수 없다")
    void create_3단계_B010() {
        Category top = Category.create(null, "문학", 1);
        Category child = Category.create(top, "한국소설", 1);

        assertThat(child.isTopLevel()).isFalse();
        assertThatThrownBy(() -> Category.create(child, "단편", 1))
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
    }
}
