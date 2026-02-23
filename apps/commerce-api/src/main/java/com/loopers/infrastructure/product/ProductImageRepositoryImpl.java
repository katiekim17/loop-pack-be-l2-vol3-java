package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductImageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductImageRepositoryImpl implements ProductImageRepository {

    private final ProductImageJpaRepository productImageJpaRepository;

    @Override
    public ProductImage save(ProductImage productImage) {
        return productImageJpaRepository.save(productImage);
    }

    @Override
    public List<ProductImage> findAllByProductId(Long productId) {
        return productImageJpaRepository.findAllByProductId(productId);
    }
}