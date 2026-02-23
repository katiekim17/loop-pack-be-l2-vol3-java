package com.loopers.domain.order;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class StockDeductionService {

    private final StockRepository stockRepository;

    @Transactional
    public void deductAll(Map<Long, Quantity> deductionMap) {
        deductionMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                Long productId = entry.getKey();
                Quantity quantity = entry.getValue();

                Stock stock = stockRepository.findByProductIdWithLock(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "재고를 찾을 수 없습니다. [productId=" + productId + "]"));

                stock.deduct(quantity);
            });
    }
}
