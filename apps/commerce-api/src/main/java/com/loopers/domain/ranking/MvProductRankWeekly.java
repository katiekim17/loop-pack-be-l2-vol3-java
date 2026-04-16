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
    name = "mv_product_rank_weekly",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_mv_weekly_product_week",
        columnNames = {"product_id", "week_start"}
    ),
    indexes = @Index(name = "idx_mv_weekly_week_sales", columnList = "week_start, total_sales DESC")
)
public class MvProductRankWeekly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "total_sales", nullable = false)
    private long totalSales;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    protected MvProductRankWeekly() {}

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public LocalDate getWeekStart() { return weekStart; }
    public long getTotalSales() { return totalSales; }
}
