package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductHistoryRepository {

    ProductHistory save(ProductHistory history);

    Page<ProductHistory> findAllByProductId(Long productId, Pageable pageable);

    int countByProductId(Long productId);
}
