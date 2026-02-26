package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderDetailInfo(
    Long orderId,
    String status,
    Long totalAmount,
    ZonedDateTime createdAt,
    List<OrderItemInfo> items
) {

    public static OrderDetailInfo from(Order order, List<OrderItem> items) {
        return new OrderDetailInfo(
            order.getId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            items.stream().map(OrderItemInfo::from).toList()
        );
    }
}
