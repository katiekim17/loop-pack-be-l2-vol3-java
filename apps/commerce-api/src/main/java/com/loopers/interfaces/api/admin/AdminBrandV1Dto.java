package com.loopers.interfaces.api.admin;

import com.loopers.application.brand.AdminBrandInfo;
import java.time.ZonedDateTime;

public class AdminBrandV1Dto {

    public record CreateBrandRequest(String name, String description, String logoImageUrl) {}

    public record UpdateBrandRequest(String name, String description, String logoImageUrl) {}

    public record AdminBrandResponse(
        Long brandId,
        String name,
        String description,
        String logoImageUrl,
        String status,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
    ) {
        public static AdminBrandResponse from(AdminBrandInfo info) {
            return new AdminBrandResponse(
                info.brandId(),
                info.name(),
                info.description(),
                info.logoImageUrl(),
                info.status(),
                info.createdAt(),
                info.updatedAt()
            );
        }
    }
}
