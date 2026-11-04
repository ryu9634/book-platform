package com.ryu.ebook.category.domain;

import com.ryu.ebook.global.common.BaseTimeEntity;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "CATEGORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CATEGORY_SEQ_GENERATOR")
    @SequenceGenerator(name = "CATEGORY_SEQ_GENERATOR", sequenceName = "CATEGORY_SEQ", allocationSize = 50)
    @Column(name = "CATEGORY_ID")
    private Long id;

    // 같은 애그리거트 안의 자기 참조라 객체로 참조합니다. DB에는 FK를 만들지 않습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category parent;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int sortOrder;

    private Category(Category parent, String name, int sortOrder) {
        this.parent = parent;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    // 분류는 2단계(예: 문학 > 한국소설)까지만 만듭니다.
    public static Category create(Category parent, String name, int sortOrder) {
        if (parent != null && parent.getParent() != null) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
        }
        return new Category(parent, name, sortOrder);
    }

    public boolean isTopLevel() {
        return parent == null;
    }

    public void rename(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
