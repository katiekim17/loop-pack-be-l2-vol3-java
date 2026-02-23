package com.loopers.domain.stock;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "stock",
    uniqueConstraints = @UniqueConstraint(name = "uk_stock_product_id", columnNames = "product_id")
)
public class Stock extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    protected Stock() {}

    public Stock(Long productId, Long quantity) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 비어있을 수 없습니다.");
        }
        if (quantity == null || quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public boolean hasEnoughStock(Quantity amount) {
        return this.quantity >= amount.getValue();
    }

    public void deduct(Quantity amount) {
        if (!hasEnoughStock(amount)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다. [productId=" + productId + "]");
        }
        this.quantity -= amount.getValue();
    }
}
