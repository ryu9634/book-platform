package com.ryu.library.category.repository;

import com.ryu.library.category.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();

    boolean existsByParentId(Long parentId);
}
