package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {

    public record CreateOrderRequest(
        List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
        Long productId,
        Long quantity
    ) {}

    public record CreateOrderResponse(
        Long orderId,
        String status,
        Long totalAmount,
        ZonedDateTime createdAt
    ) {
        public static CreateOrderResponse from(OrderInfo info) {
            return new CreateOrderResponse(
                info.orderId(),
                info.status().name(),
                info.totalAmount(),
                info.createdAt()
            );
        }
    }
}
