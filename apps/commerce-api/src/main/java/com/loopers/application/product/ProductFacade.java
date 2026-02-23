package com.loopers.application.product;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductService.ProductDetail;
import com.loopers.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;

    public ProductDetailInfo getProductDetail(Long productId) {
        ProductDetail detail = productService.getProductDetail(productId);
        return ProductDetailInfo.from(detail.product(), detail.brand(), detail.options(), detail.images());
    }

    public Page<ProductListInfo> getProductList(Long brandId, String sort, int page, int size) {
        ProductSortType sortType = ProductSortType.from(sort);
        return productService.getProductList(brandId, sortType, PageRequest.of(page, size))
            .map(ProductListInfo::from);
    }
}