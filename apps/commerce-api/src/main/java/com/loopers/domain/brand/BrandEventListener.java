package com.loopers.domain.brand;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductHistory;
import com.loopers.domain.product.ProductHistoryRepository;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class BrandEventListener {

    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBrandDeactivated(BrandDeactivatedEvent event) {
        List<Product> products = productRepository.findAllByBrandId(event.brandId());
        for (Product product : products) {
            product.deactivate();
            productRepository.save(product);
            int version = productHistoryRepository.countByProductId(product.getId()) + 1;
            productHistoryRepository.save(ProductHistory.snapshot(product, version, "system"));
        }
    }
}
