package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.StockDeductionService;
import com.loopers.infrastructure.stock.StockJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockDeductionServiceIntegrationTest {

    @Autowired
    private StockDeductionService stockDeductionService;

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class DeductAll {

        @DisplayName("모든 상품의 재고가 충분하면, 정상적으로 모두 차감된다.")
        @Test
        void deductsAll_whenAllStocksAreSufficient() {
            // arrange
            Stock stockA = stockJpaRepository.save(new Stock(1L, 100L));
            Stock stockB = stockJpaRepository.save(new Stock(2L, 50L));

            // act
            stockDeductionService.deductAll(Map.of(
                stockA.getProductId(), new Quantity(30L),
                stockB.getProductId(), new Quantity(20L)
            ));

            // assert
            assertThat(stockJpaRepository.findById(stockA.getId()).get().getQuantity()).isEqualTo(70L);
            assertThat(stockJpaRepository.findById(stockB.getId()).get().getQuantity()).isEqualTo(30L);
        }

        @DisplayName("하나라도 재고가 부족하면, BAD_REQUEST 예외가 발생하고 모든 차감이 롤백된다.")
        @Test
        void rollsBackAll_whenAnyStockIsInsufficient() {
            // arrange — stockA 충분, stockB 부족
            Stock stockA = stockJpaRepository.save(new Stock(1L, 100L));
            Stock stockB = stockJpaRepository.save(new Stock(2L, 50L));

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                stockDeductionService.deductAll(Map.of(
                    stockA.getProductId(), new Quantity(80L),   // stockA: 충분
                    stockB.getProductId(), new Quantity(60L)    // stockB: 부족
                ))
            );

            // assert — 예외 타입 확인
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            // assert — stockA도 롤백되어 원래대로 (All-or-Nothing)
            assertThat(stockJpaRepository.findById(stockA.getId()).get().getQuantity()).isEqualTo(100L);
        }

        @DisplayName("productId가 역순으로 요청되어도, 오름차순으로 정렬되어 처리된다.")
        @Test
        void deductsInAscendingOrder_whenProductIdsAreInReverseOrder() {
            // arrange — productId가 큰 것(2)을 먼저 Map에 넣어도
            Stock stockA = stockJpaRepository.save(new Stock(1L, 100L));
            Stock stockB = stockJpaRepository.save(new Stock(2L, 50L));

            // act — 순서와 무관하게 정상 차감되어야 함
            stockDeductionService.deductAll(Map.of(
                stockB.getProductId(), new Quantity(10L),  // productId=2 먼저
                stockA.getProductId(), new Quantity(30L)   // productId=1 나중
            ));

            // assert — 순서와 무관하게 모두 차감됨
            assertThat(stockJpaRepository.findById(stockA.getId()).get().getQuantity()).isEqualTo(70L);
            assertThat(stockJpaRepository.findById(stockB.getId()).get().getQuantity()).isEqualTo(40L);
        }
    }
}
