package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import java.time.ZonedDateTime;

public record CouponInfo(
    Long couponId,
    String name,
    String type,
    int value,
    int minOrderAmount,
    ZonedDateTime expiredAt,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {
    public static CouponInfo from(Coupon coupon) {
        return new CouponInfo(
            coupon.getId(),
            coupon.getName(),
            coupon.getType().name(),
            coupon.getValue(),
            coupon.getMinOrderAmount(),
            coupon.getExpiredAt(),
            coupon.getCreatedAt(),
            coupon.getUpdatedAt()
        );
    }
}
