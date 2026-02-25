package com.loopers.interfaces.api.admin;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.AdminBrandResponse;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.CreateBrandRequest;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.UpdateBrandRequest;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.AdminProductResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.CreateProductRequest;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.PageResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.ProductHistoryResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.UpdateProductRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Admin", description = "어드민 브랜드/상품 API")
public interface AdminV1ApiSpec {

    // ─────────────────────────────────────────────
    // 브랜드 관리
    // ─────────────────────────────────────────────

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AdminBrandResponse> createBrand(@RequestBody CreateBrandRequest request);

    @PutMapping("/brands/{brandId}")
    ApiResponse<AdminBrandResponse> updateBrand(@PathVariable Long brandId, @RequestBody UpdateBrandRequest request);

    @DeleteMapping("/brands/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivateBrand(@PathVariable Long brandId);

    @GetMapping("/brands/{brandId}")
    ApiResponse<AdminBrandResponse> getBrand(@PathVariable Long brandId);

    @GetMapping("/brands")
    ApiResponse<PageResponse<AdminBrandResponse>> getBrandList(
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );

    // ─────────────────────────────────────────────
    // 상품 관리
    // ─────────────────────────────────────────────

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AdminProductResponse> createProduct(@RequestBody CreateProductRequest request);

    @PutMapping("/products/{productId}")
    ApiResponse<AdminProductResponse> updateProduct(@PathVariable Long productId, @RequestBody UpdateProductRequest request);

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivateProduct(@PathVariable Long productId);

    @GetMapping("/products/{productId}")
    ApiResponse<AdminProductResponse> getProduct(@PathVariable Long productId);

    @GetMapping("/products")
    ApiResponse<PageResponse<AdminProductResponse>> getProductList(
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );

    @GetMapping("/products/{productId}/history")
    ApiResponse<PageResponse<ProductHistoryResponse>> getProductHistory(
        @PathVariable Long productId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );
}
