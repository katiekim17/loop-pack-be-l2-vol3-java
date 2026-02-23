package com.loopers.domain.order;

import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class ProductSnapshot {

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Embedded
    private Money price;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    protected ProductSnapshot() {}

    public ProductSnapshot(String productName, Money price, String brandName) {
        if (productName == null || productName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 비어있을 수 없습니다.");
        }
        if (brandName == null || brandName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.");
        }
        this.productName = productName;
        this.price = price;
        this.brandName = brandName;
    }

    public String getProductName() {
        return productName;
    }

    public Long getProductPrice() {
        return price.getValue();
    }

    public String getBrandName() {
        return brandName;
    }
}
