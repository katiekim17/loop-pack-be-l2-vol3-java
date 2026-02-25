package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import java.time.ZonedDateTime;

public record AdminBrandInfo(
    Long brandId,
    String name,
    String description,
    String logoImageUrl,
    String status,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {
    public static AdminBrandInfo from(Brand brand) {
        return new AdminBrandInfo(
            brand.getId(),
            brand.getName(),
            brand.getDescription(),
            brand.getLogoImageUrl(),
            brand.getStatus().name(),
            brand.getCreatedAt(),
            brand.getUpdatedAt()
        );
    }
}
