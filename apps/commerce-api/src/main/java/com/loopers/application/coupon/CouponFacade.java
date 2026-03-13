package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponFacade {

    private final CouponService couponService;
    private final UserCouponService userCouponService;
    private final UserService userService;

    // ─────────────────────────────────────────────
    // Admin: 쿠폰 템플릿 CRUD (FR-1~4)
    // ─────────────────────────────────────────────

    public CouponInfo createCoupon(String name, String type, int value, int minOrderAmount, ZonedDateTime expiredAt) {
        CouponType couponType = CouponType.valueOf(type);
        Coupon coupon = couponService.createCoupon(name, couponType, value, minOrderAmount, expiredAt);
        return CouponInfo.from(coupon);
    }

    public CouponInfo updateCoupon(Long couponId, String name, String requestedType, Integer requestedValue, int minOrderAmount, ZonedDateTime expiredAt) {
        CouponType parsedType = requestedType != null ? CouponType.valueOf(requestedType) : null;
        Coupon coupon = couponService.updateCoupon(couponId, name, parsedType, requestedValue, minOrderAmount, expiredAt);
        return CouponInfo.from(coupon);
    }

    public void deleteCoupon(Long couponId) {
        couponService.deleteCoupon(couponId);
    }

    public CouponInfo getCoupon(Long couponId) {
        return CouponInfo.from(couponService.getCoupon(couponId));
    }

    public Page<CouponInfo> getCoupons(int page, int size) {
        return couponService.getCoupons(PageRequest.of(page, size))
            .map(CouponInfo::from);
    }

    // ─────────────────────────────────────────────
    // Admin: 발급 내역 조회 (FR-5)
    // ─────────────────────────────────────────────

    public Page<UserCouponInfo> getIssuedCoupons(Long couponId, int page, int size) {
        couponService.getCoupon(couponId); // validate coupon exists
        return userCouponService.getIssuedCoupons(couponId, PageRequest.of(page, size))
            .map(UserCouponInfo::from);
    }

    // ─────────────────────────────────────────────
    // Customer: 쿠폰 발급 (FR-6)
    // ─────────────────────────────────────────────

    public UserCouponInfo issueCoupon(String loginId, String password, Long couponId) {
        Users user = userService.authenticate(loginId, password);
        Coupon coupon = couponService.getCouponForIssue(couponId);
        UserCoupon userCoupon = userCouponService.issueCoupon(user.getId(), coupon);
        return UserCouponInfo.from(userCoupon);
    }

    // ─────────────────────────────────────────────
    // Customer: 내 쿠폰 목록 조회 (FR-7)
    // ─────────────────────────────────────────────

    public Page<UserCouponInfo> getMyCoupons(String loginId, String password, int page, int size) {
        Users user = userService.authenticate(loginId, password);
        return userCouponService.getMyCoupons(user.getId(), PageRequest.of(page, size))
            .map(UserCouponInfo::from);
    }
}
