package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderTest {

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsOrder_whenValidInfoIsProvided() {
            // arrange & act
            Order order = new Order(1L, new Money(100000L), OrderStatus.CREATED);

            // assert
            assertAll(
                () -> assertThat(order.getMemberId()).isEqualTo(1L),
                () -> assertThat(order.getTotalAmount()).isEqualTo(100000L),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED)
            );
        }

        @DisplayName("memberId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenMemberIdIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Order(null, new Money(100000L), OrderStatus.CREATED)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("totalAmount가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTotalAmountIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Order(1L, null, OrderStatus.CREATED)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
