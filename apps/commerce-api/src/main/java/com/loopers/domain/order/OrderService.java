package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.Product;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final StockDeductionService stockDeductionService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createOrder(
        Long memberId,
        List<Product> products,
        Map<Long, Brand> brandMap,
        Map<Long, Quantity> deductionMap
    ) {
        stockDeductionService.deductAll(deductionMap);

        long totalAmount = products.stream()
            .mapToLong(p -> p.getPrice() * deductionMap.get(p.getId()).getValue())
            .sum();

        Order order = orderRepository.save(new Order(memberId, new Money(totalAmount), OrderStatus.CREATED));

        List<OrderItem> orderItems = products.stream()
            .map(product -> {
                Brand brand = brandMap.get(product.getBrandId());
                Quantity quantity = deductionMap.get(product.getId());
                ProductSnapshot snapshot = new ProductSnapshot(
                    product.getName(),
                    new Money(product.getPrice()),
                    brand.getName()
                );
                return new OrderItem(order.getId(), product.getId(), OrderItemStatus.ORDERED, snapshot, quantity);
            })
            .toList();

        orderItemRepository.saveAll(orderItems);

        return order;
    }
}
