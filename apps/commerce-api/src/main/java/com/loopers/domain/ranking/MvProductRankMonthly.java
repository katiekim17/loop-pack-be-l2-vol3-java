package com.loopers.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "mv_product_rank_monthly",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_mv_monthly_product_month",
        columnNames = {"product_id", "month_start"}
    ),
    indexes = @Index(name = "idx_mv_monthly_month_sales", columnList = "month_start, total_sales DESC")
)
public class MvProductRankMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "total_sales", nullable = false)
    private long totalSales;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    protected MvProductRankMonthly() {}

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public LocalDate getMonthStart() { return monthStart; }
    public long getTotalSales() { return totalSales; }
}
