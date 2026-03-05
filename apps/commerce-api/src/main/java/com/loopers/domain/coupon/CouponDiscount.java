package com.loopers.domain.coupon;

public record CouponDiscount(long discountAmount, long finalPrice) {

    public static CouponDiscount of(CouponType type, int value, long orderAmount) {
        long discount = switch (type) {
            case FIXED -> Math.min(orderAmount, value);
            case RATE -> orderAmount - (long) Math.floor(orderAmount * (1.0 - value / 100.0));
        };
        return new CouponDiscount(discount, orderAmount - discount);
    }
}
