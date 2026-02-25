package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAllByIds(List<Long> ids);

    List<Product> findAllByBrandId(Long brandId);

    Page<Product> findAll(Long brandId, ProductSortType sortType, Pageable pageable);

    Page<Product> findAll(Long brandId, ProductSortType sortType, List<ProductStatus> statuses, Pageable pageable);
}
