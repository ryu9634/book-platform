package com.ryu.ebook.admin.ebook.dto;

import com.ryu.ebook.ebook.domain.Ebook;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class EbookForm {

    @Pattern(regexp = "^$|^\\d{13}$", message = "ISBN은 숫자 13자리입니다.")
    private String isbn;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 200자 이하입니다.")
    private String title;

    @NotBlank(message = "저자를 입력해 주세요.")
    @Size(max = 100, message = "저자는 100자 이하입니다.")
    private String author;

    @Size(max = 100, message = "출판사는 100자 이하입니다.")
    private String publisher;

    @Size(max = 4000, message = "소개는 4000자 이하입니다.")
    private String description;

    @Size(max = 500, message = "표지 URL은 500자 이하입니다.")
    private String coverImageUrl;

    @NotNull(message = "정가를 입력해 주세요.")
    @Min(value = 0, message = "정가는 0원 이상입니다.")
    @Max(value = 999_999_999, message = "정가가 너무 큽니다.")
    private Long price;

    @NotNull(message = "판매가를 입력해 주세요.")
    @Min(value = 0, message = "판매가는 0원 이상입니다.")
    private Long salePrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedDate;

    private Long categoryId;

    public static EbookForm from(Ebook ebook) {
        EbookForm form = new EbookForm();
        form.isbn = ebook.getIsbn();
        form.title = ebook.getTitle();
        form.author = ebook.getAuthor();
        form.publisher = ebook.getPublisher();
        form.description = ebook.getDescription();
        form.coverImageUrl = ebook.getCoverImageUrl();
        form.price = ebook.getPrice();
        form.salePrice = ebook.getSalePrice();
        form.publishedDate = ebook.getPublishedDate();
        form.categoryId = ebook.getCategoryId();
        return form;
    }

    public String normalizedIsbn() {
        return isbn == null || isbn.isBlank() ? null : isbn.trim();
    }
}
