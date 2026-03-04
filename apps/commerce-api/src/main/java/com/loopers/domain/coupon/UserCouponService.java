package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public UserCoupon issueCoupon(Long userId, Coupon coupon) {
        if (coupon.isExpired()) {
            throw new CoreException(ErrorType.COUPON_EXPIRED);
        }
        if (userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())) {
            throw new CoreException(ErrorType.COUPON_ALREADY_ISSUED);
        }
        return userCouponRepository.save(new UserCoupon(userId, coupon.getId(), coupon.getName()));
    }

    @Transactional(readOnly = true)
    public Page<UserCoupon> getIssuedCoupons(Long couponId, Pageable pageable) {
        return userCouponRepository.findAllByCouponId(couponId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserCoupon> getMyCoupons(Long userId, Pageable pageable) {
        return userCouponRepository.findAllByUserId(userId, pageable);
    }

    /**
     * 주문 시 쿠폰 유효성 검증 후 사용 처리.
     * - 소유권, 상태, 만료 여부, 최소 주문 금액 검증
     * - 통과 시 할인 금액 계산 후 UserCoupon.status → USED
     */
    @Transactional
    public CouponDiscount validateAndUse(Long userCouponId, Long userId, long orderAmount) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
            .orElseThrow(() -> new CoreException(ErrorType.USER_COUPON_NOT_FOUND));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.COUPON_OWNER_MISMATCH);
        }

        if (userCoupon.getStatus() != UserCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.COUPON_NOT_AVAILABLE);
        }

        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
            .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND));

        if (coupon.isExpired()) {
            userCoupon.markAsExpired();
            userCouponRepository.save(userCoupon);
            throw new CoreException(ErrorType.COUPON_EXPIRED);
        }

        if (orderAmount < coupon.getMinOrderAmount()) {
            throw new CoreException(ErrorType.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        CouponDiscount discount = CouponDiscount.of(coupon.getType(), coupon.getValue(), orderAmount);
        userCoupon.markAsUsed();
        userCouponRepository.save(userCoupon);
        return discount;
    }
}
