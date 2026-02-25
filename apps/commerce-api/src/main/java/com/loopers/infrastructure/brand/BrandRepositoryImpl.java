package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BrandRepositoryImpl implements BrandRepository {

  private final BrandJpaRepository brandJpaRepository;

  @Override
  public Brand save(Brand brand) {
    return brandJpaRepository.save(brand);
  }

  @Override
  public Optional<Brand> findById(Long id) {
    return brandJpaRepository.findById(id);
  }

  @Override
  public List<Brand> findAllByIds(List<Long> ids) {
    return brandJpaRepository.findAllByIdIn(ids);
  }

  @Override
  public Page<Brand> findAll(BrandStatus status, Pageable pageable) {
    // 기본 정렬: createdAt 내림차순
    Pageable sortedPageable = PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "createdAt")
    );
    return brandJpaRepository.findAllByStatusFilter(status, sortedPageable);
  }
}
