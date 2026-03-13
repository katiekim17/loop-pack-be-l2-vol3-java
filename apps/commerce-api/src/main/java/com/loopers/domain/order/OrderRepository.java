package com.loopers.domain.order;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    // 특정 회원의 주문 목록 페이징 조회 (최신순)
    Page<Order> findAllByMemberId(Long memberId, Pageable pageable);
}
