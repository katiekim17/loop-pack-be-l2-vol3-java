package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_product_metrics_product_date",
        columnNames = {"product_id", "metric_date"}
    )
)
public class ProductMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    protected ProductMetrics() {}

    public ProductMetrics(Long productId, LocalDate metricDate, long salesCount) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.salesCount = salesCount;
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public LocalDate getMetricDate() { return metricDate; }
    public long getSalesCount() { return salesCount; }
}
