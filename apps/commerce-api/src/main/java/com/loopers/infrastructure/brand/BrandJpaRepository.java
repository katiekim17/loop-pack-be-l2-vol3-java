package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {

    List<Brand> findAllByIdIn(List<Long> ids);
}
