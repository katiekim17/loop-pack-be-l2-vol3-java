package com.loopers.application.product;

import com.loopers.domain.product.Product;
import java.time.ZonedDateTime;

public record AdminProductInfo(
    Long productId,
    String name,
    String description,
    Long brandId,
    String thumbnailImageUrl,
    Long price,
    String status,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {
    public static AdminProductInfo from(Product product) {
        return new AdminProductInfo(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getBrandId(),
            product.getThumbnailImageUrl(),
            product.getPrice(),
            product.getStatus().name(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
