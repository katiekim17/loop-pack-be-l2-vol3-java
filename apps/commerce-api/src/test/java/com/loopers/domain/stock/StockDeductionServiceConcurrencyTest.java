package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.StockDeductionService;
import com.loopers.infrastructure.stock.StockJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockDeductionServiceConcurrencyTest {

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

    @DisplayName("재고 차감 동시성")
    @Nested
    class DeductAllConcurrency {

        @DisplayName("재고가 10개일 때 동시에 10개씩 차감 요청하면, 정확히 1건만 성공하고 재고는 0이 된다.")
        @Test
        void deductsExactlyOnce_whenConcurrentRequestsExceedStock() throws InterruptedException {
            // arrange - 재고 10개
            Stock stock = stockJpaRepository.save(new Stock(1L, 10L));
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act - 5개 스레드가 각각 10개씩 차감 시도 (재고는 10개이므로 1건만 성공 가능)
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockDeductionService.deductAll(Map.of(stock.getProductId(), new Quantity(10L)));
                        successCount.incrementAndGet();
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert - 정확히 1건 성공, 재고는 0
            assertThat(successCount.get()).isEqualTo(1);
            long remainingQuantity = stockJpaRepository.findById(stock.getId()).orElseThrow().getQuantity();
            assertThat(remainingQuantity).isEqualTo(0L);
        }

        @DisplayName("재고가 100개일 때 N개 스레드가 동시에 각 1개씩 차감하면, 재고는 정확히 (100 - N)이 된다.")
        @Test
        void deductsExactQuantity_whenConcurrentSmallDeductions() throws InterruptedException {
            // arrange - 재고 100개
            Stock stock = stockJpaRepository.save(new Stock(1L, 100L));
            int threadCount = 50;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // act - 50개 스레드가 동시에 각 1개씩 차감
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockDeductionService.deductAll(Map.of(stock.getProductId(), new Quantity(1L)));
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert - oversell 없이 정확히 50 차감
            long remainingQuantity = stockJpaRepository.findById(stock.getId()).orElseThrow().getQuantity();
            assertThat(remainingQuantity).isEqualTo(50L);
        }
    }
}