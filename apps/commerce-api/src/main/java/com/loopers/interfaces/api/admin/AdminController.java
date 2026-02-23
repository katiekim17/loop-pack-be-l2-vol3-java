package com.loopers.interfaces.api.admin;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.brand.BrandService;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandV1Dto;
import com.loopers.interfaces.api.brand.BrandV1Dto.BrandResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.interfaces.api.product.ProductV1Dto.ProductListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1")
public class AdminController implements AdminV1ApiSpec {
  // (GET) /api-admin/v1/brands?page=0&size=20  // 등록된 브랜드 목록 조회

  // (POST) /api-admin/v1/brands  // 브랜드 등록
  // (PUT) /api-admin/v1/brands/{brandId}  // 브랜드 정보 수정
  // (DELETE) /api-admin/v1/brands/{brandId}  // 브랜드 삭제


  // (POST) /api-admin/v1/products  // 상품 등록
  // (PUT) /api-admin/v1/products/{productId}  // 상품 정보 수정
  // (DELETE) /api-admin/v1/products/{productId}  // 상품 삭제
  // (POST) /api-admin/v1/orders  // 주문 요청
  // (GET) /api-admin/v1/orders?startAt=2026-01-31&endAt=2026-02-10  // 유저의 주문 목록 조회
  // (GET) /api-admin/v1/orders/{orderId}  // 단일 주문 상세 조회

  private final BrandService brandService;
  private final ProductFacade productFacade;



  // (GET) /api-admin/v1/brands/{brandId} // 브랜드 상세 조회
  @GetMapping("/brands/{brandId}")
  @Override
  public ApiResponse<BrandResponse> getBrands(
      @PathVariable(value = "brandId") Long brandId
  ) {
    BrandInfo info = brandService.getBrandInfo(brandId);
    BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(info);
    return ApiResponse.success(response);
  }


  // (GET) /api-admin/v1/products?page=0&size=20&brandId={ brandId}  // 등록된 상품 목록 조회
  @GetMapping("/products")
  @Override
  public ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductListItemResponse>> getProductList(
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false, defaultValue = "latest") String sort,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "20") int size
  ) {
    Page<ProductListItemResponse> responsePage = productFacade.getProductList(brandId, sort, page, size)
        .map(ProductV1Dto.ProductListItemResponse::from);
    return ApiResponse.success(ProductV1Dto.PageResponse.from(responsePage));
  }

  // (GET) /api-admin/v1/products/{productId}  // 상품 상세 조회
  @GetMapping("/products/{productId}")
  @Override
  public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(
      @PathVariable(value = "productId") Long productId
  ) {
    return ApiResponse.success(
        ProductV1Dto.ProductDetailResponse.from(productFacade.getProductDetail(productId))
    );
  }

}
