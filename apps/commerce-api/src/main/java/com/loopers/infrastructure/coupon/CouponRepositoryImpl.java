package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id);
    }

    @Override
    public Page<Coupon> findAllNotDeleted(Pageable pageable) {
        Pageable sorted = PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return couponJpaRepository.findAllNotDeleted(sorted);
    }
}
