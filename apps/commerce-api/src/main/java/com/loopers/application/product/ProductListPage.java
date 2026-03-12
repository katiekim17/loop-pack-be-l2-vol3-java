package com.loopers.application.product;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public record ProductListPage(
    List<ProductListInfo> content,
    int page,
    int size,
    long totalElements
) {
    public static ProductListPage from(Page<ProductListInfo> page) {
        return new ProductListPage(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    public Page<ProductListInfo> toPage() {
        return new PageImpl<>(content, PageRequest.of(page, size), totalElements);
    }
}