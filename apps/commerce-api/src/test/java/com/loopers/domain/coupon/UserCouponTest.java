package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserCouponTest {

    @DisplayName("발급 쿠폰을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 정보가 주어지면, AVAILABLE 상태로 생성된다.")
        @Test
        void createsUserCoupon_withAvailableStatus() {
            UserCoupon userCoupon = new UserCoupon(1L, 10L, "10% 할인 쿠폰");

            assertAll(
                () -> assertThat(userCoupon.getUserId()).isEqualTo(1L),
                () -> assertThat(userCoupon.getCouponId()).isEqualTo(10L),
                () -> assertThat(userCoupon.getCouponName()).isEqualTo("10% 할인 쿠폰"),
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE),
                () -> assertThat(userCoupon.getUsedAt()).isNull()
            );
        }

        @DisplayName("userId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenUserIdIsNull() {
            CoreException result = assertThrows(CoreException.class, () ->
                new UserCoupon(null, 10L, "쿠폰")
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("couponId가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponIdIsNull() {
            CoreException result = assertThrows(CoreException.class, () ->
                new UserCoupon(1L, null, "쿠폰")
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("couponName이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponNameIsBlank() {
            CoreException result = assertThrows(CoreException.class, () ->
                new UserCoupon(1L, 10L, "   ")
            );
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰을 사용 처리할 때,")
    @Nested
    class MarkAsUsed {

        @DisplayName("markAsUsed() 호출 시, 상태가 USED로 변경되고 usedAt이 설정된다.")
        @Test
        void changesStatusToUsed_andSetsUsedAt() {
            UserCoupon userCoupon = new UserCoupon(1L, 10L, "쿠폰");

            userCoupon.markAsUsed();

            assertAll(
                () -> assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED),
                () -> assertThat(userCoupon.getUsedAt()).isNotNull()
            );
        }
    }

    @DisplayName("쿠폰을 만료 처리할 때,")
    @Nested
    class MarkAsExpired {

        @DisplayName("markAsExpired() 호출 시, 상태가 EXPIRED로 변경된다.")
        @Test
        void changesStatusToExpired() {
            UserCoupon userCoupon = new UserCoupon(1L, 10L, "쿠폰");

            userCoupon.markAsExpired();

            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
        }
    }
}
