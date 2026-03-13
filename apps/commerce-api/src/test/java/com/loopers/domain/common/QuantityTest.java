package com.loopers.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QuantityTest {

    @DisplayName("수량을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("양수이면, 정상적으로 생성된다.")
        @Test
        void createsQuantity_whenValueIsPositive() {
            // arrange & act
            Quantity quantity = new Quantity(5L);

            // assert
            assertThat(quantity.getValue()).isEqualTo(5L);
        }

        @DisplayName("0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsZero() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Quantity(0L)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Quantity(-1L)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Quantity(null)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}