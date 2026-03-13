package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.domain.brand.Brand;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LikeServiceConcurrencyTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeJpaRepository likeJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록 동시성")
    @Nested
    class AddLikeConcurrency {

        @DisplayName("같은 유저가 같은 상품에 동시에 N번 좋아요를 누르면, Like 레코드는 1건만 저장된다.")
        @Test
        void savesOnlyOneLike_whenSameUserLikesConcurrently() throws InterruptedException {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            long userId = 1L;
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        likeService.addLike(userId, product.getId());
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert
            long savedCount = likeJpaRepository.findAll().stream()
                .filter(l -> l.getProductId().equals(product.getId()))
                .count();
            assertThat(savedCount).isEqualTo(1L);
        }

        @DisplayName("N명이 동시에 같은 상품에 좋아요를 누르면, likeCount가 정확히 N이어야 한다.")
        @Test
        void likeCountEqualsN_whenNUsersLikeConcurrently() throws InterruptedException {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            product.activate();
            productJpaRepository.save(product);
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // act - 각 스레드는 서로 다른 userId로 좋아요
            for (int i = 0; i < threadCount; i++) {
                long userId = (long) (i + 1);
                executor.submit(() -> {
                    try {
                        likeService.addLike(userId, product.getId());
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert - 비동기 이벤트 처리 완료 대기 후 likeCount 검증
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                long likeCount = productJpaRepository.findById(product.getId())
                    .orElseThrow().getLikeCount();
                assertThat(likeCount).isEqualTo(threadCount);
            });
        }
    }

    @DisplayName("좋아요 취소 동시성")
    @Nested
    class RemoveLikeConcurrency {

        @DisplayName("같은 유저가 동시에 N번 좋아요 취소를 요청해도, likeCount는 정확히 0이어야 한다.")
        @Test
        void likeCountIsZero_whenSameUserRemovesConcurrently() throws InterruptedException {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            product.activate();
            productJpaRepository.save(product);
            long userId = 1L;
            likeService.addLike(userId, product.getId());

            // likeCount가 1이 될 때까지 대기
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                long likeCount = productJpaRepository.findById(product.getId())
                    .orElseThrow().getLikeCount();
                assertThat(likeCount).isEqualTo(1L);
            });

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // act - 동시에 N번 취소
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        likeService.removeLike(userId, product.getId());
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert - 비동기 이벤트 처리 완료 대기 후 likeCount == 0
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                long likeCount = productJpaRepository.findById(product.getId())
                    .orElseThrow().getLikeCount();
                assertThat(likeCount).isEqualTo(0L);
            });
        }
    }
}