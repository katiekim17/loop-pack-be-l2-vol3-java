package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        return productJpaRepository.findAllByIdIn(ids);
    }

    @Override
    public List<Product> findAllByBrandId(Long brandId) {
        return productJpaRepository.findAllByBrandId(brandId);
    }

    @Override
    public Page<Product> findAll(Long brandId, ProductSortType sortType, Pageable pageable) {
        return findAll(brandId, sortType, null, pageable);
    }

    @Override
    public Page<Product> findAll(Long brandId, ProductSortType sortType, List<ProductStatus> statuses, Pageable pageable) {
        QProduct product = QProduct.product;
        BooleanBuilder where = new BooleanBuilder();

        if (brandId != null) {
            where.and(product.brandId.eq(brandId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            where.and(product.status.in(statuses));
        }

        OrderSpecifier<?> order = resolveOrderSpecifier(sortType);

        List<Product> content = queryFactory
            .selectFrom(product)
            .where(where)
            .orderBy(order)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(product.count())
            .from(product)
            .where(where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private OrderSpecifier<?> resolveOrderSpecifier(ProductSortType sortType) {
        QProduct product = QProduct.product;
        return switch (sortType) {
            case PRICE_ASC -> product.price.value.asc();
            case LIKES_DESC -> product.likeCount.desc();
            default -> product.createdAt.desc();
        };
    }
}
