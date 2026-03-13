package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByIdIn(List<Long> ids);

    List<Product> findAllByBrandId(Long brandId);

    @Query("SELECT p FROM Product p WHERE (:brandId IS NULL OR p.brandId = :brandId)")
    Page<Product> findAllByBrandIdFilter(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (:brandId IS NULL OR p.brandId = :brandId) AND p.status IN :statuses")
    Page<Product> findAllByStatusFilter(
        @Param("brandId") Long brandId,
        @Param("statuses") List<ProductStatus> statuses,
        Pageable pageable
    );
}