package com.ryu.ebook.category.repository;

import com.ryu.ebook.category.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();

    boolean existsByParentId(Long parentId);
}
