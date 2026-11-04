package com.ryu.library.book.domain;

import com.ryu.library.global.common.BaseTimeEntity;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 실제 책 한 권. 같은 Book 애그리거트 안이라 Book을 객체로 참조합니다(FK 없음, cascade 없음).
// 두 회원이 같은 권을 동시에 대출하지 못하도록 @Version(낙관적 락)을 둡니다.
@Getter
@Entity
@Table(name = "BOOK_COPY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookCopy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOOK_COPY_SEQ_GENERATOR")
    @SequenceGenerator(name = "BOOK_COPY_SEQ_GENERATOR", sequenceName = "BOOK_COPY_SEQ", allocationSize = 50)
    @Column(name = "BOOK_COPY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOK_ID", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Book book;

    @Column(nullable = false, length = 30)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCopyStatus status;

    @Version
    private Long version;

    private BookCopy(Book book, String barcode) {
        this.book = book;
        this.barcode = barcode;
        this.status = BookCopyStatus.AVAILABLE;
    }

    public static BookCopy create(Book book, String barcode) {
        return new BookCopy(book, barcode);
    }

    // 바코드: LIB-{도서ID 6자리}-{권 번호 3자리}. 예: LIB-000012-003
    public static String barcodeOf(Long bookId, long copyNumber) {
        return "LIB-%06d-%03d".formatted(bookId, copyNumber);
    }

    public boolean isAvailable() {
        return status == BookCopyStatus.AVAILABLE;
    }

    public void lend() {
        if (status != BookCopyStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COPY_NOT_AVAILABLE);
        }
        this.status = BookCopyStatus.ON_LOAN;
    }

    // 예약자에게 넘긴 권은 예약자만 대출할 수 있습니다. 그 확인은 대출 서비스가 합니다.
    public void lendToReserver() {
        if (status != BookCopyStatus.RESERVED) {
            throw new BusinessException(ErrorCode.COPY_NOT_AVAILABLE);
        }
        this.status = BookCopyStatus.ON_LOAN;
    }

    public void returnToShelf() {
        requireStatus(BookCopyStatus.ON_LOAN);
        this.status = BookCopyStatus.AVAILABLE;
    }

    public void holdForReservation() {
        if (status != BookCopyStatus.ON_LOAN && status != BookCopyStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
        }
        this.status = BookCopyStatus.RESERVED;
    }

    public void releaseReservation() {
        requireStatus(BookCopyStatus.RESERVED);
        this.status = BookCopyStatus.AVAILABLE;
    }

    // 관리자 상태 변경: 서가에 있는 권만 분실/폐기 처리합니다. 대출 중인 권은 반납 처리 후 변경합니다.
    public void markLost() {
        requireStatus(BookCopyStatus.AVAILABLE);
        this.status = BookCopyStatus.LOST;
    }

    public void discard() {
        if (status != BookCopyStatus.AVAILABLE && status != BookCopyStatus.LOST) {
            throw new BusinessException(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
        }
        this.status = BookCopyStatus.DISCARDED;
    }

    public void restoreFound() {
        requireStatus(BookCopyStatus.LOST);
        this.status = BookCopyStatus.AVAILABLE;
    }

    private void requireStatus(BookCopyStatus expected) {
        if (status != expected) {
            throw new BusinessException(ErrorCode.COPY_STATUS_CHANGE_NOT_ALLOWED);
        }
    }
}
