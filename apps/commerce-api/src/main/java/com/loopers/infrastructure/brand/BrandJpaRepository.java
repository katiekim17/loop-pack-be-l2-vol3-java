package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {

    List<Brand> findAllByIdIn(List<Long> ids);

    // status가 null이면 전체, 아니면 해당 상태만 반환
    @Query("SELECT b FROM Brand b WHERE (:status IS NULL OR b.status = :status)")
    Page<Brand> findAllByStatusFilter(@Param("status") BrandStatus status, Pageable pageable);
}
