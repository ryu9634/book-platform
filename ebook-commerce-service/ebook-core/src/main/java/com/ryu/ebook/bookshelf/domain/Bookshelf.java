package com.ryu.ebook.bookshelf.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

// 내 서재: 결제로 얻은 소유권. 회원당 전자책 하나(MEMBER_ID, EBOOK_ID UNIQUE).
// 환불하면 행은 남기고 비활성으로 바꾸며, 다시 사면 같은 행을 활성으로 되돌립니다.
@Getter
@Entity
@Table(name = "BOOKSHELF")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookshelf extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOOKSHELF_SEQ_GENERATOR")
    @SequenceGenerator(name = "BOOKSHELF_SEQ_GENERATOR", sequenceName = "BOOKSHELF_SEQ", allocationSize = 50)
    @Column(name = "BOOKSHELF_ID")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long ebookId;

    // 소유권의 근거가 된 주문 항목. 환불할 때 이 값으로 어느 주문에서 온 소유권인지 찾습니다.
    @Column(nullable = false)
    private Long orderItemId;

    @Column(nullable = false)
    private LocalDateTime acquiredAt;

    @Column(nullable = false)
    private int lastReadPage;

    private LocalDateTime lastReadAt;

    // 컬럼은 NUMBER(1)(0/1)입니다. Hibernate 7은 Oracle 23에서 boolean을 BOOLEAN 타입으로 기대하므로 숫자로 바꿔 저장합니다.
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active;

    private Bookshelf(Long memberId, Long ebookId, Long orderItemId, LocalDateTime acquiredAt) {
        this.memberId = memberId;
        this.ebookId = ebookId;
        this.orderItemId = orderItemId;
        this.acquiredAt = acquiredAt;
        this.active = true;
    }

    public static Bookshelf acquire(Long memberId, Long ebookId, Long orderItemId, LocalDateTime now) {
        return new Bookshelf(memberId, ebookId, orderItemId, now);
    }

    // 환불 후 다시 산 경우: 새 주문 항목을 근거로 다시 활성화합니다. 읽은 위치는 처음부터입니다.
    public void reacquire(Long orderItemId, LocalDateTime now) {
        this.orderItemId = orderItemId;
        this.acquiredAt = now;
        this.lastReadPage = 0;
        this.lastReadAt = null;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean hasBeenRead() {
        return lastReadAt != null;
    }

    public void recordReading(int page, LocalDateTime now) {
        this.lastReadPage = page;
        this.lastReadAt = now;
    }
}
