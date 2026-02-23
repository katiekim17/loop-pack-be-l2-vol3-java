package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    public OrderInfo createOrder(String loginId, String password, List<OrderItemCommand> items) {
        Users user = userService.authenticate(loginId, password);

        List<Long> productIds = items.stream().map(OrderItemCommand::productId).toList();
        List<Product> products = productService.getProducts(productIds);

        List<Long> brandIds = products.stream().map(Product::getBrandId).distinct().toList();
        List<Brand> brands = productService.getBrands(brandIds);

        Map<Long, Brand> brandMap = brands.stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));
        Map<Long, Quantity> deductionMap = items.stream()
            .collect(Collectors.toMap(OrderItemCommand::productId, cmd -> new Quantity(cmd.quantity())));

        Order order = orderService.createOrder(user.getId(), products, brandMap, deductionMap);

        return OrderInfo.from(order);
    }
}
