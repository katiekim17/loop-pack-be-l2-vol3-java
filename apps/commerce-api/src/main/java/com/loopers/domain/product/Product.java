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
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
    private Money price;

    @Column(name = "description")
    private String description;

    @Column(name = "thumbnail_image_url", length = 500)
    private String thumbnailImageUrl;

    protected Product() {}

    public Product(Long brandId, String name, Money price, String description) {
        this(brandId, name, price, description, null);
    }

    public Product(Long brandId, String name, Money price, String description, String thumbnailImageUrl) {
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 비어있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 비어있을 수 없습니다.");
        }
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public Long getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price.getValue();
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }
}
