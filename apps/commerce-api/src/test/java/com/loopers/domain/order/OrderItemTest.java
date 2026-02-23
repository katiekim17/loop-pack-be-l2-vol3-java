package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    private static final ProductSnapshot VALID_SNAPSHOT =
        new ProductSnapshot("나이키 신발", new Money(50000L), "나이키");

    @DisplayName("주문 항목을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsOrderItem_whenValidInfoIsProvided() {
            // arrange & act
            OrderItem orderItem = new OrderItem(1L, 1L, OrderItemStatus.ORDERED, VALID_SNAPSHOT, new Quantity(2L));

            // assert
            assertAll(
                () -> assertThat(orderItem.getOrderId()).isEqualTo(1L),
                () -> assertThat(orderItem.getProductId()).isEqualTo(1L),
                () -> assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.ORDERED),
                () -> assertThat(orderItem.getQuantity()).isEqualTo(2L)
            );
        }

        @DisplayName("orderId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenOrderIdIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new OrderItem(null, 1L, OrderItemStatus.ORDERED, VALID_SNAPSHOT, new Quantity(2L))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new OrderItem(1L, null, OrderItemStatus.ORDERED, VALID_SNAPSHOT, new Quantity(2L))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("snapshot이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenSnapshotIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new OrderItem(1L, 1L, OrderItemStatus.ORDERED, null, new Quantity(2L))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
