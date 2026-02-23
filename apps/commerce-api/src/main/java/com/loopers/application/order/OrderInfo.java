package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import java.time.ZonedDateTime;

public record OrderInfo(Long orderId, OrderStatus status, Long totalAmount, ZonedDateTime createdAt) {

    public static OrderInfo from(Order order) {
        return new OrderInfo(
            order.getId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
}
