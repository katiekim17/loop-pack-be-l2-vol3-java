package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.alert.AlertNotifier;
import com.loopers.domain.common.Money;
import com.loopers.domain.coupon.UserCouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderItemRepository orderItemRepository;

    @MockBean
    private UserCouponService userCouponService;

    @MockBean
    private AlertNotifier alertNotifier;

    @DisplayName("createOrder() 에서 트랜잭션이 롤백될 때,")
    @Nested
    class CreateOrderRollback {

        @DisplayName("주문 생성 실패로 롤백되면, 알림을 발송한다.")
        @Test
        void notifiesAlert_whenCreateOrderRollsBack() {
            // arrange
            Users mockUser = mock(Users.class);
            when(mockUser.getId()).thenReturn(1L);
            when(userService.authenticate(any(), any())).thenReturn(mockUser);
            when(productService.getProducts(any())).thenReturn(List.of());
            when(productService.getBrands(any())).thenReturn(List.of());
            when(orderService.createOrder(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB 저장 실패"));

            // act
            assertThatThrownBy(() ->
                orderFacade.createOrder("loginId", "password", List.of(), null)
            ).isInstanceOf(RuntimeException.class);

            // assert
            verify(alertNotifier).notify(anyString(), anyString());
        }

        @DisplayName("주문 생성이 성공하면, 알림을 발송하지 않는다.")
        @Test
        void doesNotNotifyAlert_whenCreateOrderSucceeds() {
            // arrange
            Users mockUser = mock(Users.class);
            when(mockUser.getId()).thenReturn(1L);
            when(userService.authenticate(any(), any())).thenReturn(mockUser);
            when(productService.getProducts(any())).thenReturn(List.of());
            when(productService.getBrands(any())).thenReturn(List.of());
            Order order = new Order(1L, new Money(0L), OrderStatus.CREATED);
            when(orderService.createOrder(any(), any(), any(), any(), any(), any())).thenReturn(order);

            // act
            orderFacade.createOrder("loginId", "password", List.of(), null);

            // assert
            verify(alertNotifier, never()).notify(anyString(), anyString());
        }
    }
}