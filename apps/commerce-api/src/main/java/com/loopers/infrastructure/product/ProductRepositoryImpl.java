package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
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
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        return productJpaRepository.findAllByIdIn(ids);
    }

    @Override
    public Page<Product> findAll(Long brandId, ProductSortType sortType, Pageable pageable) {
        Sort sort = switch (sortType) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price.value");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return productJpaRepository.findAllByBrandIdFilter(brandId, sortedPageable);
    }
}