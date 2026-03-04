package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class CouponServiceIntegrationTest {

    private static final ZonedDateTime FUTURE = ZonedDateTime.now().plusYears(1);

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 템플릿 등록 시,")
    @Nested
    class CreateCoupon {

        @DisplayName("유효한 정보가 주어지면, 쿠폰이 저장된다.")
        @Test
        void savesCoupon_whenValidInfoIsProvided() {
            Coupon coupon = couponService.createCoupon("신규 10% 할인", CouponType.RATE, 10, 10000, FUTURE);

            assertAll(
                () -> assertThat(coupon.getId()).isNotNull(),
                () -> assertThat(coupon.getName()).isEqualTo("신규 10% 할인"),
                () -> assertThat(coupon.getType()).isEqualTo(CouponType.RATE),
                () -> assertThat(coupon.getValue()).isEqualTo(10),
                () -> assertThat(coupon.getDeletedAt()).isNull()
            );
        }
    }

    @DisplayName("쿠폰 템플릿 수정 시,")
    @Nested
    class UpdateCoupon {

        @DisplayName("유효한 정보로 수정하면, 변경이 반영된다.")
        @Test
        void updatesCoupon_whenValidInfoIsProvided() {
            Coupon saved = couponJpaRepository.save(new Coupon("원래 이름", CouponType.FIXED, 1000, 5000, FUTURE));
            ZonedDateTime newExpiry = ZonedDateTime.now().plusMonths(6);

            Coupon updated = couponService.updateCoupon(saved.getId(), "새 이름", null, null, 10000, newExpiry);

            assertAll(
                () -> assertThat(updated.getName()).isEqualTo("새 이름"),
                () -> assertThat(updated.getMinOrderAmount()).isEqualTo(10000),
                () -> assertThat(updated.getType()).isEqualTo(CouponType.FIXED),
                () -> assertThat(updated.getValue()).isEqualTo(1000)
            );
        }

        @DisplayName("type이 포함되면, COUPON_TYPE_IMMUTABLE 예외가 발생한다.")
        @Test
        void throwsCouponTypeImmutable_whenTypeIsProvided() {
            Coupon saved = couponJpaRepository.save(new Coupon("쿠폰", CouponType.FIXED, 1000, 0, FUTURE));

            CoreException result = assertThrows(CoreException.class, () ->
                couponService.updateCoupon(saved.getId(), "새 이름", CouponType.RATE, null, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.COUPON_TYPE_IMMUTABLE);
        }

        @DisplayName("value가 포함되면, COUPON_TYPE_IMMUTABLE 예외가 발생한다.")
        @Test
        void throwsCouponTypeImmutable_whenValueIsProvided() {
            Coupon saved = couponJpaRepository.save(new Coupon("쿠폰", CouponType.FIXED, 1000, 0, FUTURE));

            CoreException result = assertThrows(CoreException.class, () ->
                couponService.updateCoupon(saved.getId(), "새 이름", null, 2000, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.COUPON_TYPE_IMMUTABLE);
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 수정하면, COUPON_NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsCouponNotFound_whenCouponDoesNotExist() {
            CoreException result = assertThrows(CoreException.class, () ->
                couponService.updateCoupon(99999L, "이름", null, null, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.COUPON_NOT_FOUND);
        }
    }

    @DisplayName("쿠폰 템플릿 삭제 시,")
    @Nested
    class DeleteCoupon {

        @DisplayName("정상 삭제 시, deletedAt이 설정된다.")
        @Test
        void setsDeletdAt_whenDeleted() {
            Coupon saved = couponJpaRepository.save(new Coupon("쿠폰", CouponType.FIXED, 1000, 0, FUTURE));

            couponService.deleteCoupon(saved.getId());

            Coupon deleted = couponJpaRepository.findById(saved.getId()).orElseThrow();
            assertThat(deleted.getDeletedAt()).isNotNull();
        }

        @DisplayName("이미 삭제된 쿠폰을 재삭제하면, COUPON_NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsCouponNotFound_whenAlreadyDeleted() {
            Coupon saved = couponJpaRepository.save(new Coupon("쿠폰", CouponType.FIXED, 1000, 0, FUTURE));
            couponService.deleteCoupon(saved.getId());

            CoreException result = assertThrows(CoreException.class, () ->
                couponService.deleteCoupon(saved.getId())
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.COUPON_NOT_FOUND);
        }
    }

    @DisplayName("쿠폰 템플릿 목록 조회 시,")
    @Nested
    class GetCoupons {

        @DisplayName("삭제되지 않은 쿠폰만 조회된다.")
        @Test
        void returnsOnlyNotDeletedCoupons() {
            couponJpaRepository.save(new Coupon("쿠폰1", CouponType.FIXED, 1000, 0, FUTURE));
            Coupon couponToDelete = couponJpaRepository.save(new Coupon("쿠폰2", CouponType.RATE, 10, 0, FUTURE));
            couponService.deleteCoupon(couponToDelete.getId());

            Page<Coupon> result = couponService.getCoupons(PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("쿠폰1");
        }
    }
}
