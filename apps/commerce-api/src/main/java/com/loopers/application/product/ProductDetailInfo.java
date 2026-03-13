package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductOption;
import java.time.ZonedDateTime;
import java.util.List;

public record ProductDetailInfo(
    Long productId,
    String name,
    String description,
    BrandInfo brand,
    List<String> imageUrls,
    List<OptionInfo> options,
    long likeCount,
    ZonedDateTime createdAt
) {

    public record OptionInfo(Long optionId, String name, Long price, Long stockQuantity, boolean isAvailable) {}

    public static ProductDetailInfo from(
        Product product,
        Brand brand,
        List<ProductOption> options,
        List<ProductImage> images
    ) {
        return new ProductDetailInfo(
            product.getId(),
            product.getName(),
            product.getDescription(),
            BrandInfo.from(brand),
            images.stream().map(ProductImage::getImageUrl).toList(),
            options.stream()
                .map(o -> new OptionInfo(o.getId(), o.getName(), o.getPrice(), o.getStockQuantity(), o.isAvailable()))
                .toList(),
            product.getLikeCount(),
            product.getCreatedAt()
        );
    }
}