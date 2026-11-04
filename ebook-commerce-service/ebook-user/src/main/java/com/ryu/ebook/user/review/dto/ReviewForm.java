package com.ryu.ebook.user.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewForm {

    @NotNull(message = "별점을 골라 주세요.")
    @Min(value = 1, message = "별점은 1~5점입니다.")
    @Max(value = 5, message = "별점은 1~5점입니다.")
    private Integer rating;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 2000, message = "내용은 2000자 이하입니다.")
    private String content;

    public static ReviewForm of(int rating, String content) {
        ReviewForm form = new ReviewForm();
        form.rating = rating;
        form.content = content;
        return form;
    }
}
