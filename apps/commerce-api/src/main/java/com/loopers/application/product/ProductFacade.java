package com.loopers.application.product;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductService.ProductDetail;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private static final List<ProductStatus> CUSTOMER_VISIBLE_STATUSES =
        List.of(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK);

    private final ProductService productService;

    public ProductDetailInfo getProductDetail(Long productId) {
        ProductDetail detail = productService.getProductDetail(productId);
        return ProductDetailInfo.from(detail.product(), detail.brand(), detail.options(), detail.images());
    }

    public Page<ProductListInfo> getProductList(Long brandId, String sort, int page, int size) {
        ProductSortType sortType = ProductSortType.from(sort);
        return productService.getProductList(brandId, sortType, CUSTOMER_VISIBLE_STATUSES, PageRequest.of(page, size))
            .map(ProductListInfo::from);
    }
}