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

class ProductSnapshotTest {

    @DisplayName("상품 스냅샷을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsProductSnapshot_whenValidInfoIsProvided() {
            // arrange & act
            ProductSnapshot snapshot = new ProductSnapshot("나이키 신발", new Money(50000L), "나이키");

            // assert
            assertAll(
                () -> assertThat(snapshot.getProductName()).isEqualTo("나이키 신발"),
                () -> assertThat(snapshot.getProductPrice()).isEqualTo(50000L),
                () -> assertThat(snapshot.getBrandName()).isEqualTo("나이키")
            );
        }

        @DisplayName("상품명이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductNameIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new ProductSnapshot(null, new Money(50000L), "나이키")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("가격이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenPriceIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new ProductSnapshot("나이키 신발", null, "나이키")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("브랜드명이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBrandNameIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new ProductSnapshot("나이키 신발", new Money(50000L), null)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
