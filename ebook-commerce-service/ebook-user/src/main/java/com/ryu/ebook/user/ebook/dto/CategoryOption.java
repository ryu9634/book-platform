package com.ryu.ebook.user.ebook.dto;

import com.ryu.ebook.category.domain.Category;

public record CategoryOption(Long id, String name) {

    public static CategoryOption from(Category category) {
        return new CategoryOption(category.getId(), category.getName());
    }
}
