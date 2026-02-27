package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;

public record OrderItemInfo(
    Long orderItemId,
    Long productId,
    String productName,
    String brandName,
    Long price,
    Long quantity,
    String status
) {

    public static OrderItemInfo from(OrderItem item) {
        return new OrderItemInfo(
            item.getId(),
            item.getProductId(),
            item.getSnapshot().getProductName(),
            item.getSnapshot().getBrandName(),
            item.getSnapshot().getProductPrice(),
            item.getQuantity(),
            item.getStatus().name()
        );
    }
}
