package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import java.time.ZonedDateTime;

public record BrandInfo(Long brandId, String name, String description, String logoImageUrl, ZonedDateTime createdAt) {
    public static BrandInfo from(Brand model) {
        return new BrandInfo(
            model.getId(),
            model.getName(),
            model.getDescription(),
            model.getLogoImageUrl(),
            model.getCreatedAt()
        );
    }
}
