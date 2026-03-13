package com.loopers.interfaces.api.admin;

import com.loopers.application.product.AdminProductInfo;
import com.loopers.application.product.ProductHistoryInfo;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public class AdminProductV1Dto {

    public record CreateProductRequest(Long brandId, String name, Long price, String description, String thumbnailImageUrl) {}

    public record UpdateProductRequest(Long brandId, String name, Long price, String description, String thumbnailImageUrl) {}

    public record AdminProductResponse(
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
        public static AdminProductResponse from(AdminProductInfo info) {
            return new AdminProductResponse(
                info.productId(),
                info.name(),
                info.description(),
                info.brandId(),
                info.thumbnailImageUrl(),
                info.price(),
                info.status(),
                info.createdAt(),
                info.updatedAt()
            );
        }
    }

    public record ProductHistoryResponse(
        Long historyId,
        Integer version,
        String name,
        Long price,
        String status,
        String changedBy,
        ZonedDateTime changedAt
    ) {
        public static ProductHistoryResponse from(ProductHistoryInfo info) {
            return new ProductHistoryResponse(
                info.historyId(),
                info.version(),
                info.name(),
                info.price(),
                info.status(),
                info.changedBy(),
                info.changedAt()
            );
        }
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
            );
        }
    }
}
