package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItemRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import com.loopers.interfaces.api.order.OrderV1Dto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    public OrderInfo createOrder(String loginId, String password, List<OrderV1Dto.OrderItemRequest> items) {
        Users user = userService.authenticate(loginId, password);

        List<Long> productIds = items.stream().map(OrderV1Dto.OrderItemRequest::productId).toList();
        List<Product> products = productService.getProducts(productIds);

        List<Long> brandIds = products.stream().map(Product::getBrandId).distinct().toList();
        List<Brand> brands = productService.getBrands(brandIds);

        Map<Long, Brand> brandMap = brands.stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));
        Map<Long, Quantity> deductionMap = items.stream()
            .collect(Collectors.toMap(OrderV1Dto.OrderItemRequest::productId, item -> new Quantity(item.quantity())));

        Order order = orderService.createOrder(user.getId(), products, brandMap, deductionMap);

        return OrderInfo.from(order);
    }

    // 본인의 주문 목록을 최신순 페이징으로 반환한다.
    public Page<OrderInfo> getOrderList(String loginId, String password, int page, int size) {
        Users user = userService.authenticate(loginId, password);
        return orderService.getOrderList(user.getId(), PageRequest.of(page, size))
            .map(OrderInfo::from);
    }

    /**
     * 주문 단건 상세 조회.
     * OrderService에서 소유권 검증 후, OrderItem 목록을 함께 반환한다.
     */
    public OrderDetailInfo getOrderDetail(String loginId, String password, Long orderId) {
        Users user = userService.authenticate(loginId, password);
        Order order = orderService.getOrder(user.getId(), orderId);
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        return OrderDetailInfo.from(order, items);
    }
}
