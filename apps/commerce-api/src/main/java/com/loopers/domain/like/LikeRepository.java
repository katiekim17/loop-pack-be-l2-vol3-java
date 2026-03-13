package com.loopers.domain.like;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeRepository {

    Like save(Like like);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    // 특정 유저의 좋아요 목록 (최신순 페이징)
    Page<Like> findAllByUserId(Long userId, Pageable pageable);
}
