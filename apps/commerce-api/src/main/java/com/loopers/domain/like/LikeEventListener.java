package com.loopers.domain.like;

import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class LikeEventListener {

    private final ProductRepository productRepository;

    /**
     * 좋아요 등록 커밋 후 비동기로 like_count 증가.
     * REQUIRES_NEW: 새 트랜잭션을 사용해 이벤트 리스너가 독립적으로 커밋한다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCreated(LikeCreatedEvent event) {
        productRepository.findById(event.productId()).ifPresent(product -> {
            product.incrementLikeCount();
            productRepository.save(product);
        });
    }

    /**
     * 좋아요 취소 커밋 후 비동기로 like_count 감소.
     * 최솟값 0 보장은 Product.decrementLikeCount()에서 처리한다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeDeleted(LikeDeletedEvent event) {
        productRepository.findById(event.productId()).ifPresent(product -> {
            product.decrementLikeCount();
            productRepository.save(product);
        });
    }
}
