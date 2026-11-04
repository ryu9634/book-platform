package com.ryu.ebook.review.repository;

public record ReviewSummary(double averageRating, long count) {

    public String averageText() {
        return "%.1f".formatted(averageRating);
    }
}
