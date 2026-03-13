package com.loopers.domain.brand;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandRepository {

    Brand save(Brand brand);

    Optional<Brand> findById(Long id);

    List<Brand> findAllByIds(List<Long> ids);

    // 어드민 브랜드 목록 조회: status가 null이면 전체, 아니면 해당 상태만 반환
    Page<Brand> findAll(BrandStatus status, Pageable pageable);
}