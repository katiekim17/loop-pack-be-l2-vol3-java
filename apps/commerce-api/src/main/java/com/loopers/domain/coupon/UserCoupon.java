package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "user_coupon",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_coupon_user_id_coupon_id", columnNames = {"user_id", "coupon_id"})
    }
)
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "coupon_name", nullable = false, length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserCouponStatus status;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    protected UserCoupon() {}

    public UserCoupon(Long userId, Long couponId, String couponName) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
        }
        if (couponId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 비어있을 수 없습니다.");
        }
        if (couponName == null || couponName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 비어있을 수 없습니다.");
        }
        this.userId = userId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.status = UserCouponStatus.AVAILABLE;
    }

    public void markAsUsed() {
        this.status = UserCouponStatus.USED;
        this.usedAt = ZonedDateTime.now();
    }

    public void markAsExpired() {
        this.status = UserCouponStatus.EXPIRED;
    }

    public Long getUserId() { return userId; }
    public Long getCouponId() { return couponId; }
    public String getCouponName() { return couponName; }
    public UserCouponStatus getStatus() { return status; }
    public ZonedDateTime getUsedAt() { return usedAt; }
}
