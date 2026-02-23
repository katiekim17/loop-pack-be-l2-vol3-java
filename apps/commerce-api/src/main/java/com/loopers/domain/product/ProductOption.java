package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_option")
public class ProductOption extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
    private Money price;

    @Column(name = "stock_quantity", nullable = false)
    private Long stockQuantity;

    protected ProductOption() {}

    public ProductOption(Long productId, String name, Money price, Long stockQuantity) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 비어있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션명은 비어있을 수 없습니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 비어있을 수 없습니다.");
        }
        if (stockQuantity == null || stockQuantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public boolean isAvailable() {
        return stockQuantity > 0;
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price.getValue();
    }

    public Long getStockQuantity() {
        return stockQuantity;
    }
}