package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "brand")
public class Brand extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "logo_image_url", length = 500)
    private String logoImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BrandStatus status = BrandStatus.PENDING;

    protected Brand() {}

    public Brand(String name) {
        this(name, null, null);
    }

    public Brand(String name, String description, String logoImageUrl) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.");
        }
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
    }

    public void activate() {
        this.status = BrandStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BrandStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == BrandStatus.ACTIVE;
    }

    public void updateInfo(String name, String description, String logoImageUrl) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.");
        }
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoImageUrl() {
        return logoImageUrl;
    }

    public BrandStatus getStatus() {
        return status;
    }
}
