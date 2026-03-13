package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.stock.StockJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class CreateOrder {

        @DisplayName("모든 재고가 충분하면, Order와 OrderItem이 생성되고 재고가 차감된다.")
        @Test
        void createsOrderAndItems_whenAllStocksAreSufficient() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));

            Product productA = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명A"));
            Product productB = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(30000L), "설명B"));

            stockJpaRepository.save(new Stock(productA.getId(), 100L));
            stockJpaRepository.save(new Stock(productB.getId(), 50L));

            Long memberId = 1L;
            Map<Long, Quantity> deductionMap = Map.of(
                productA.getId(), new Quantity(2L),
                productB.getId(), new Quantity(3L)
            );
            Map<Long, Brand> brandMap = Map.of(brand.getId(), brand);

            // act
            Order order = orderService.createOrder(memberId, List.of(productA, productB), brandMap, deductionMap);

            // assert — Order 검증
            assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getMemberId()).isEqualTo(memberId),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED),
                () -> assertThat(order.getTotalAmount()).isEqualTo(50000L * 2 + 30000L * 3) // 190000
            );

            // assert — OrderItem 검증
            List<OrderItem> orderItems = orderItemJpaRepository.findAll();
            assertThat(orderItems).hasSize(2);
            assertThat(orderItems).allMatch(item -> item.getStatus() == OrderItemStatus.ORDERED);

            // assert — 재고 차감 검증
            assertThat(stockJpaRepository.findAll())
                .extracting(Stock::getQuantity)
                .containsExactlyInAnyOrder(98L, 47L); // 100-2, 50-3
        }

        @DisplayName("하나라도 재고가 부족하면, BAD_REQUEST 예외가 발생하고 Order가 생성되지 않는다.")
        @Test
        void throwsBadRequest_whenAnyStockIsInsufficient() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));

            Product productA = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명A"));
            Product productB = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(30000L), "설명B"));

            stockJpaRepository.save(new Stock(productA.getId(), 100L));
            stockJpaRepository.save(new Stock(productB.getId(), 5L));   // productB 재고 부족

            Long memberId = 1L;
            Map<Long, Quantity> deductionMap = Map.of(
                productA.getId(), new Quantity(2L),
                productB.getId(), new Quantity(10L)  // 5 < 10 → 부족
            );
            Map<Long, Brand> brandMap = Map.of(brand.getId(), brand);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                orderService.createOrder(memberId, List.of(productA, productB), brandMap, deductionMap)
            );

            // assert — 예외 타입
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

            // assert — Order 미생성, 재고 원상복구
            assertThat(orderJpaRepository.count()).isZero();
            assertThat(stockJpaRepository.findAll())
                .extracting(Stock::getQuantity)
                .containsExactlyInAnyOrder(100L, 5L);
        }

        @DisplayName("totalAmount는 각 상품의 가격 × 수량의 합산이다.")
        @Test
        void calculatesTotalAmount_asSum_ofPriceTimesQuantity() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));

            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(25000L), "설명"));
            stockJpaRepository.save(new Stock(product.getId(), 100L));

            Map<Long, Quantity> deductionMap = Map.of(product.getId(), new Quantity(4L));
            Map<Long, Brand> brandMap = Map.of(brand.getId(), brand);

            // act
            Order order = orderService.createOrder(1L, List.of(product), brandMap, deductionMap);

            // assert — 25000 × 4 = 100000
            assertThat(order.getTotalAmount()).isEqualTo(100000L);
        }
    }
}
