package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderItemStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "productName", column = @Column(name = "product_name", nullable = false, length = 200)),
        @AttributeOverride(name = "price.value", column = @Column(name = "product_price", nullable = false)),
        @AttributeOverride(name = "brandName", column = @Column(name = "brand_name", nullable = false, length = 100))
    })
    private ProductSnapshot snapshot;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    private Quantity quantity;

    protected OrderItem() {}

    public OrderItem(Long orderId, Long productId, OrderItemStatus status,
                     ProductSnapshot snapshot, Quantity quantity) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 비어있을 수 없습니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 비어있을 수 없습니다.");
        }
        if (snapshot == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 스냅샷은 비어있을 수 없습니다.");
        }
        this.orderId = orderId;
        this.productId = productId;
        this.status = status;
        this.snapshot = snapshot;
        this.quantity = quantity;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public Long getQuantity() {
        return quantity.getValue();
    }

    public ProductSnapshot getSnapshot() {
        return snapshot;
    }
}
