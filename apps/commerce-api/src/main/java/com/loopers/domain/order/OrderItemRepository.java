package com.loopers.domain.order;

import java.util.List;

public interface OrderItemRepository {

    List<OrderItem> saveAll(List<OrderItem> orderItems);

    // 특정 주문의 아이템 목록 조회
    List<OrderItem> findAllByOrderId(Long orderId);
}
