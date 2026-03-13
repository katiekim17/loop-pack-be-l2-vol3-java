package com.loopers.application.product;

import com.loopers.domain.product.ProductHistory;
import java.time.ZonedDateTime;

public record ProductHistoryInfo(
    Long historyId,
    Integer version,
    String name,
    Long price,
    String status,
    String changedBy,
    ZonedDateTime changedAt
) {
    public static ProductHistoryInfo from(ProductHistory history) {
        return new ProductHistoryInfo(
            history.getId(),
            history.getVersion(),
            history.getName(),
            history.getPrice(),
            history.getStatus(),
            history.getChangedBy(),
            history.getCreatedAt()
        );
    }
}
