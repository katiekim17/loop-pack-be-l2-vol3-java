package com.loopers.application.product;

import com.loopers.domain.product.ProductService.ProductListItem;
import java.time.ZonedDateTime;

public record ProductListInfo(
    Long productId,
    String name,
    Long brandId,
    String brandName,
    String thumbnailImageUrl,
    Long minPrice,
    long likeCount,
    ZonedDateTime createdAt
) {

    public static ProductListInfo from(ProductListItem item) {
        return new ProductListInfo(
            item.product().getId(),
            item.product().getName(),
            item.brand().getId(),
            item.brand().getName(),
            item.product().getThumbnailImageUrl(),
            item.minPrice(),
            item.product().getLikeCount(),
            item.product().getCreatedAt()
        );
    }
}