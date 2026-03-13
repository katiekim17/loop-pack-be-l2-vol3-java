package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import java.time.ZonedDateTime;

public class BrandV1Dto {
    public record BrandResponse(Long brandId, String name, String description, String logoImageUrl, ZonedDateTime createdAt) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(
                info.brandId(),
                info.name(),
                info.description(),
                info.logoImageUrl(),
                info.createdAt()
            );
        }
    }
}
