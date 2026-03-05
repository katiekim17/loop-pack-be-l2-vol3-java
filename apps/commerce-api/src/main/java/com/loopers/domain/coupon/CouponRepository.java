package com.loopers.domain.coupon;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Page<Coupon> findAllNotDeleted(Pageable pageable);
}
