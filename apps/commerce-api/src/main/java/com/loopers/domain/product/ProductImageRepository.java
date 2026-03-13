package com.loopers.domain.product;

import java.util.List;

public interface ProductImageRepository {

    ProductImage save(ProductImage productImage);

    List<ProductImage> findAllByProductId(Long productId);
}