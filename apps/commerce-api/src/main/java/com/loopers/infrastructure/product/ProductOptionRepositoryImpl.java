package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductOptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final ProductOptionJpaRepository productOptionJpaRepository;

    @Override
    public ProductOption save(ProductOption productOption) {
        return productOptionJpaRepository.save(productOption);
    }

    @Override
    public List<ProductOption> findAllByProductId(Long productId) {
        return productOptionJpaRepository.findAllByProductId(productId);
    }

    @Override
    public List<ProductOption> findAllByProductIds(List<Long> productIds) {
        return productOptionJpaRepository.findAllByProductIdIn(productIds);
    }
}