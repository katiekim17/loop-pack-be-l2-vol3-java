package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductHistory;
import com.loopers.domain.product.ProductHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductHistoryRepositoryImpl implements ProductHistoryRepository {

    private final ProductHistoryJpaRepository productHistoryJpaRepository;

    @Override
    public ProductHistory save(ProductHistory history) {
        return productHistoryJpaRepository.save(history);
    }

    @Override
    public Page<ProductHistory> findAllByProductId(Long productId, Pageable pageable) {
        return productHistoryJpaRepository.findAllByProductId(productId, pageable);
    }

    @Override
    public int countByProductId(Long productId) {
        return productHistoryJpaRepository.countByProductId(productId);
    }
}
