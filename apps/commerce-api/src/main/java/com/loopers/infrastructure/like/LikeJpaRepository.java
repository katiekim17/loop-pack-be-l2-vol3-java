package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    @Transactional
    void deleteByUserIdAndProductId(Long userId, Long productId);

    Page<Like> findAllByUserId(Long userId, Pageable pageable);
}
