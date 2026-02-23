package com.loopers.interfaces.api.admin;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandV1Dto.BrandResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Brand&Products", description = "어드민 브랜드/상품 API")
public interface AdminV1ApiSpec {

  @GetMapping("/{brandId}")
  ApiResponse<BrandResponse> getBrands(
      @PathVariable(value = "brandId") Long brandId
  );

  // (GET) /api-admin/v1/products?page=0&size=20&brandId={ brandId}  // 등록된 상품 목록 조회
  @GetMapping("/products")
  ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductListItemResponse>> getProductList(
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false, defaultValue = "latest") String sort,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "20") int size
  );

  @GetMapping("/{productId}")
  ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
      @PathVariable(value = "productId") Long productId
  );
}
