package com.loopers.application.coupon;

import com.loopers.domain.coupon.UserCoupon;
import java.time.ZonedDateTime;

public record UserCouponInfo(
    Long userCouponId,
    Long userId,
    Long couponId,
    String couponName,
    String status,
    ZonedDateTime issuedAt,
    ZonedDateTime usedAt
) {
    public static UserCouponInfo from(UserCoupon userCoupon) {
        return new UserCouponInfo(
            userCoupon.getId(),
            userCoupon.getUserId(),
            userCoupon.getCouponId(),
            userCoupon.getCouponName(),
            userCoupon.getStatus().name(),
            userCoupon.getCreatedAt(),
            userCoupon.getUsedAt()
        );
    }
}
