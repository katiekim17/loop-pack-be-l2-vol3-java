package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StockTest {

    @DisplayName("재고를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsStock_whenValidInfoIsProvided() {
            // arrange & act
            Stock stock = new Stock(1L, 100L);

            // assert
            assertThat(stock.getQuantity()).isEqualTo(100L);
        }

        @DisplayName("productId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIdIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Stock(null, 100L)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("초기 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenQuantityIsNegative() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Stock(1L, -1L)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("재고를 확인할 때, ")
    @Nested
    class HasEnoughStock {

        @DisplayName("요청 수량보다 재고가 충분하면, true를 반환한다.")
        @Test
        void returnsTrue_whenStockIsSufficient() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act & assert
            assertThat(stock.hasEnoughStock(new Quantity(50L))).isTrue();
        }

        @DisplayName("요청 수량과 재고가 같으면, true를 반환한다.")
        @Test
        void returnsTrue_whenStockEqualsRequestedQuantity() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act & assert
            assertThat(stock.hasEnoughStock(new Quantity(100L))).isTrue();
        }

        @DisplayName("요청 수량보다 재고가 부족하면, false를 반환한다.")
        @Test
        void returnsFalse_whenStockIsInsufficient() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act & assert
            assertThat(stock.hasEnoughStock(new Quantity(101L))).isFalse();
        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class Deduct {

        @DisplayName("재고가 충분하면, 수량이 차감된다.")
        @Test
        void deductsQuantity_whenStockIsSufficient() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act
            stock.deduct(new Quantity(30L));

            // assert
            assertThat(stock.getQuantity()).isEqualTo(70L);
        }

        @DisplayName("재고가 정확히 맞으면, 차감 후 0이 된다.")
        @Test
        void deductsToZero_whenStockMatchesRequestedQuantity() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act
            stock.deduct(new Quantity(100L));

            // assert
            assertThat(stock.getQuantity()).isEqualTo(0L);
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsInsufficient() {
            // arrange
            Stock stock = new Stock(1L, 100L);

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                stock.deduct(new Quantity(101L))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
