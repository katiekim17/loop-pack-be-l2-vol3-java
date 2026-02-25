package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductHistoryJpaRepository extends JpaRepository<ProductHistory, Long> {

    Page<ProductHistory> findAllByProductId(Long productId, Pageable pageable);

    int countByProductId(Long productId);
}
