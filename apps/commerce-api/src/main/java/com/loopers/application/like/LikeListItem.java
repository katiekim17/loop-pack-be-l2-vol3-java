package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.Like;
import com.loopers.domain.product.Product;
import java.time.ZonedDateTime;

public record LikeListItem(
    Long productId,
    String name,
    Long brandId,
    String brandName,
    String thumbnailImageUrl,
    Long minPrice,
    long likeCount,
    ZonedDateTime likedAt
) {

    public static LikeListItem from(Like like, Product product, Brand brand) {
        return new LikeListItem(
            product.getId(),
            product.getName(),
            brand.getId(),
            brand.getName(),
            product.getThumbnailImageUrl(),
            product.getPrice(),
            product.getLikeCount(),
            like.getCreatedAt()
        );
    }
}
