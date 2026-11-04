package com.ryu.library.admin.book.dto;

import com.ryu.library.category.domain.Category;

public record CategoryOption(Long id, String name) {

    public static CategoryOption from(Category category) {
        return new CategoryOption(category.getId(), category.getName());
    }
}
