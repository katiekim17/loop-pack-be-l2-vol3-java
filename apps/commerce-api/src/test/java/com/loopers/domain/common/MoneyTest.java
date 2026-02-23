package com.loopers.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @DisplayName("금액을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("양수 금액이면, 정상적으로 생성된다.")
        @Test
        void createsMoney_whenValueIsPositive() {
            // arrange & act
            Money money = new Money(1000L);

            // assert
            assertThat(money.getValue()).isEqualTo(1000L);
        }

        @DisplayName("0원이면, 정상적으로 생성된다.")
        @Test
        void createsMoney_whenValueIsZero() {
            // arrange & act
            Money money = new Money(0L);

            // assert
            assertThat(money.getValue()).isEqualTo(0L);
        }

        @DisplayName("음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNegative() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Money(-1L)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Money(null)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}