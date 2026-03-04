package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CouponTest {

    private static final ZonedDateTime FUTURE = ZonedDateTime.now().plusYears(1);

    @DisplayName("쿠폰을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        void createsCoupon_whenValidInfoIsProvided() {
            Coupon coupon = new Coupon("10% 할인", CouponType.RATE, 10, 10000, FUTURE);

            assertAll(
                () -> assertThat(coupon.getName()).isEqualTo("10% 할인"),
                () -> assertThat(coupon.getType()).isEqualTo(CouponType.RATE),
                () -> assertThat(coupon.getValue()).isEqualTo(10),
                () -> assertThat(coupon.getMinOrderAmount()).isEqualTo(10000),
                () -> assertThat(coupon.getExpiredAt()).isEqualTo(FUTURE)
            );
        }

        @DisplayName("name이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon(null, CouponType.RATE, 10, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("name이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("   ", CouponType.RATE, 10, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("type이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenTypeIsNull() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("쿠폰", null, 10, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("FIXED 타입에서 value가 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenFixedValueIsZero() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("쿠폰", CouponType.FIXED, 0, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("RATE 타입에서 value가 101이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenRateValueExceeds100() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("쿠폰", CouponType.RATE, 101, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("minOrderAmount가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenMinOrderAmountIsNegative() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("쿠폰", CouponType.FIXED, 1000, -1, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("expiredAt이 현재 시각 이전이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenExpiredAtIsInThePast() {
            CoreException result = assertThrows(CoreException.class, () ->
                new Coupon("쿠폰", CouponType.FIXED, 1000, 0, ZonedDateTime.now().minusDays(1))
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰을 수정할 때,")
    @Nested
    class Update {

        @DisplayName("유효한 정보로 수정하면, 값이 변경된다.")
        @Test
        void updatesCoupon_whenValidInfoIsProvided() {
            Coupon coupon = new Coupon("원래 이름", CouponType.FIXED, 1000, 5000, FUTURE);
            ZonedDateTime newExpiry = ZonedDateTime.now().plusMonths(6);

            coupon.update("새 이름", 10000, newExpiry);

            assertAll(
                () -> assertThat(coupon.getName()).isEqualTo("새 이름"),
                () -> assertThat(coupon.getMinOrderAmount()).isEqualTo(10000),
                () -> assertThat(coupon.getExpiredAt()).isEqualTo(newExpiry)
            );
        }

        @DisplayName("name이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            Coupon coupon = new Coupon("원래 이름", CouponType.FIXED, 1000, 0, FUTURE);

            CoreException result = assertThrows(CoreException.class, () ->
                coupon.update(null, 0, FUTURE)
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("type은 불변이므로 변경 시도 시 type/value는 그대로 유지된다.")
        @Test
        void typeAndValueRemainUnchanged_afterUpdate() {
            Coupon coupon = new Coupon("쿠폰", CouponType.RATE, 10, 0, FUTURE);

            coupon.update("새 이름", 0, FUTURE);

            assertAll(
                () -> assertThat(coupon.getType()).isEqualTo(CouponType.RATE),
                () -> assertThat(coupon.getValue()).isEqualTo(10)
            );
        }
    }

    @DisplayName("쿠폰 만료 여부 확인 시,")
    @Nested
    class IsExpired {

        @DisplayName("만료 일시가 지나지 않았으면, false를 반환한다.")
        @Test
        void returnsFalse_whenNotExpired() {
            Coupon coupon = new Coupon("쿠폰", CouponType.FIXED, 1000, 0, FUTURE);
            assertThat(coupon.isExpired()).isFalse();
        }
    }
}
