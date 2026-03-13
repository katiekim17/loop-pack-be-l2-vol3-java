package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOption, Long> {

    List<ProductOption> findAllByProductId(Long productId);

    List<ProductOption> findAllByProductIdIn(List<Long> productIds);
}