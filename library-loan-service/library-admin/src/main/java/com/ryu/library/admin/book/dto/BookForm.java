package com.ryu.library.admin.book.dto;

import com.ryu.library.admin.book.client.NlBookInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

// 관리자 도서 등록 폼. Thymeleaf th:field 바인딩을 위해 record 대신 getter/setter를 둡니다.
@Getter
@Setter
@NoArgsConstructor
public class BookForm {

    @NotBlank(message = "ISBN을 입력해 주세요.")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN은 숫자 13자리입니다.")
    private String isbn;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 200자 이하입니다.")
    private String title;

    @NotBlank(message = "저자를 입력해 주세요.")
    @Size(max = 100, message = "저자는 100자 이하입니다.")
    private String author;

    @Size(max = 100, message = "출판사는 100자 이하입니다.")
    private String publisher;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedDate;

    @Size(max = 2000, message = "소개는 2000자 이하입니다.")
    private String description;

    @Size(max = 500, message = "표지 URL은 500자 이하입니다.")
    private String coverImageUrl;

    private Long categoryId;

    public static BookForm from(NlBookInfo info) {
        BookForm form = new BookForm();
        form.isbn = info.isbn();
        form.title = info.title();
        form.author = info.author();
        form.publisher = info.publisher();
        form.publishedDate = info.publishedDate();
        form.coverImageUrl = info.coverImageUrl();
        form.description = info.description();
        return form;
    }

    public static BookForm withIsbn(String isbn) {
        BookForm form = new BookForm();
        form.isbn = isbn;
        return form;
    }
}
