package com.ryu.ebook.ebook.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 판매 상품. 재고가 없고 파일 하나가 계속 팔립니다.
@Getter
@Entity
@Table(name = "EBOOK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ebook extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EBOOK_SEQ_GENERATOR")
    @SequenceGenerator(name = "EBOOK_SEQ_GENERATOR", sequenceName = "EBOOK_SEQ", allocationSize = 50)
    @Column(name = "EBOOK_ID")
    private Long id;

    // 분류는 다른 애그리거트라 ID로만 참조합니다.
    private Long categoryId;

    @Column(length = 13)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 100)
    private String publisher;

    @Column(length = 4000)
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    // 금액은 원 단위 정수입니다.
    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long salePrice;

    @Column(length = 300)
    private String fileKey;

    private Long fileSize;

    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EbookStatus status;

    private LocalDate publishedDate;

    // 인기순 정렬용 비정규화 값. 결제 완료 시 +1, 환불 시 -1.
    @Column(nullable = false)
    private long salesCount;

    @Builder
    private Ebook(Long categoryId, String isbn, String title, String author, String publisher, String description,
            String coverImageUrl, long price, long salePrice, LocalDate publishedDate) {
        validatePrice(price, salePrice);
        this.categoryId = categoryId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.price = price;
        this.salePrice = salePrice;
        this.publishedDate = publishedDate;
        this.status = EbookStatus.ON_SALE;
    }

    public void update(Long categoryId, String title, String author, String publisher, String description,
            String coverImageUrl, long price, long salePrice, LocalDate publishedDate) {
        validatePrice(price, salePrice);
        this.categoryId = categoryId;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.price = price;
        this.salePrice = salePrice;
        this.publishedDate = publishedDate;
    }

    public void attachFile(String fileKey, long fileSize, int pageCount) {
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
    }

    public void changeStatus(EbookStatus status) {
        this.status = status;
    }

    public boolean isOnSale() {
        return status == EbookStatus.ON_SALE;
    }

    public boolean isHidden() {
        return status == EbookStatus.HIDDEN;
    }

    public boolean hasFile() {
        return fileKey != null;
    }

    // 할인율(%)은 저장하지 않고 정가와 판매가로 계산합니다.
    public int discountRate() {
        return price == 0 ? 0 : (int) Math.round((price - salePrice) * 100.0 / price);
    }

    public void increaseSales() {
        this.salesCount++;
    }

    public void decreaseSales() {
        if (salesCount > 0) {
            this.salesCount--;
        }
    }

    // DB에도 같은 CHECK 제약이 있지만, 화면에 이유를 보여 주기 위해 먼저 확인합니다.
    private static void validatePrice(long price, long salePrice) {
        if (price < 0 || salePrice < 0 || salePrice > price) {
            throw new BusinessException(ErrorCode.INVALID_PRICE);
        }
    }
}
