package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.ZonedDateTime;
import java.util.List;
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
class UserCouponServiceConcurrencyTest {

    private static final ZonedDateTime FUTURE = ZonedDateTime.now().plusYears(1);

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 발급 동시성")
    @Nested
    class IssueCouponConcurrency {

        @DisplayName("같은 유저가 같은 쿠폰을 동시에 N번 발급 요청하면, UserCoupon은 1건만 저장된다.")
        @Test
        void savesOnlyOneUserCoupon_whenSameUserIssuesConcurrently() throws InterruptedException {
            // arrange
            Coupon coupon = couponJpaRepository.save(new Coupon("10% 할인", CouponType.RATE, 10, 0, FUTURE));
            long userId = 1L;
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        userCouponService.issueCoupon(userId, coupon);
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert
            long savedCount = userCouponJpaRepository.findAll().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getCouponId().equals(coupon.getId()))
                .count();
            assertThat(savedCount).isEqualTo(1L);
        }
    }

    @DisplayName("쿠폰 사용 동시성")
    @Nested
    class ValidateAndUseConcurrency {

        @DisplayName("같은 UserCoupon을 동시에 N번 사용 요청하면, 정확히 1건만 USED 처리된다.")
        @Test
        void marksAsUsedOnlyOnce_whenSameUserCouponUsedConcurrently() throws InterruptedException {
            // arrange
            Coupon coupon = couponJpaRepository.save(new Coupon("1000원 할인", CouponType.FIXED, 1000, 0, FUTURE));
            UserCoupon userCoupon = userCouponJpaRepository.save(new UserCoupon(1L, coupon.getId(), coupon.getName()));
            long userId = 1L;
            long orderAmount = 10000L;
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        userCouponService.validateAndUse(userCoupon.getId(), userId, orderAmount);
                        successCount.incrementAndGet();
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // assert - 성공은 정확히 1번, status는 USED
            assertThat(successCount.get()).isEqualTo(1);

            List<UserCoupon> usedCoupons = userCouponJpaRepository.findAll().stream()
                .filter(uc -> uc.getId().equals(userCoupon.getId())
                    && uc.getStatus() == UserCouponStatus.USED)
                .toList();
            assertThat(usedCoupons).hasSize(1);
        }
    }
}