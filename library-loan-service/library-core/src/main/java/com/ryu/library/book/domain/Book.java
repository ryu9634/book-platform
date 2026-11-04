package com.ryu.library.book.domain;

import com.ryu.library.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 서지 정보("어떤 책인가"). 실제 한 권 한 권은 BOOK_COPY로 따로 관리합니다.
@Getter
@Entity
@Table(name = "BOOK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseTimeEntity {

    static final int TITLE_MAX = 200;
    static final int AUTHOR_MAX = 100;
    static final int PUBLISHER_MAX = 100;
    static final int DESCRIPTION_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOOK_SEQ_GENERATOR")
    @SequenceGenerator(name = "BOOK_SEQ_GENERATOR", sequenceName = "BOOK_SEQ", allocationSize = 50)
    @Column(name = "BOOK_ID")
    private Long id;

    // 다른 애그리거트(분류)는 ID로만 참조합니다.
    private Long categoryId;

    @Column(length = 13)
    private String isbn;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(nullable = false, length = AUTHOR_MAX)
    private String author;

    @Column(length = PUBLISHER_MAX)
    private String publisher;

    private LocalDate publishedDate;

    @Column(length = DESCRIPTION_MAX)
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    // 외부(국립중앙도서관) 데이터는 길이가 컬럼보다 길 수 있어 저장 전에 컬럼 길이에 맞춰 자릅니다.
    @Builder
    private Book(Long categoryId, String isbn, String title, String author, String publisher,
            LocalDate publishedDate, String description, String coverImageUrl) {
        this.categoryId = categoryId;
        this.isbn = isbn;
        this.title = cut(title, TITLE_MAX);
        this.author = cut(author, AUTHOR_MAX);
        this.publisher = cut(publisher, PUBLISHER_MAX);
        this.publishedDate = publishedDate;
        this.description = cut(description, DESCRIPTION_MAX);
        this.coverImageUrl = coverImageUrl;
    }

    private static String cut(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
