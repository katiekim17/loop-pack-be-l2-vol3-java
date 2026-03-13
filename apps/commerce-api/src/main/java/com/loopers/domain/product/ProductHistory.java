package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_history")
public class ProductHistory extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    protected ProductHistory() {}

    private ProductHistory(Long productId, int version, Long brandId, String name, Long price,
        String status, String description, String changedBy) {
        this.productId = productId;
        this.version = version;
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.status = status;
        this.description = description;
        this.changedBy = changedBy;
    }

    public static ProductHistory snapshot(Product product, int version, String changedBy) {
        return new ProductHistory(
            product.getId(),
            version,
            product.getBrandId(),
            product.getName(),
            product.getPrice(),
            product.getStatus().name(),
            product.getDescription(),
            changedBy
        );
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getVersion() {
        return version;
    }

    public Long getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getChangedBy() {
        return changedBy;
    }
}
