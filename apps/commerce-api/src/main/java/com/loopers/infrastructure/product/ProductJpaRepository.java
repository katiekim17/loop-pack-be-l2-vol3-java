package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByIdIn(List<Long> ids);

    List<Product> findAllByBrandId(Long brandId);
}