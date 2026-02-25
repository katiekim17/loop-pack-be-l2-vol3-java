package com.loopers.interfaces.api.admin;

import com.loopers.application.brand.AdminBrandFacade;
import com.loopers.application.product.AdminProductFacade;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.AdminBrandResponse;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.CreateBrandRequest;
import com.loopers.interfaces.api.admin.AdminBrandV1Dto.UpdateBrandRequest;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.AdminProductResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.CreateProductRequest;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.PageResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.ProductHistoryResponse;
import com.loopers.interfaces.api.admin.AdminProductV1Dto.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 브랜드/상품 관리 API.
 * LDAP 인증은 AdminAuthInterceptor에서 body 파싱 이전에 처리하므로
 * 이 컨트롤러는 인증이 완료된 요청만 처리한다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1")
public class AdminController implements AdminV1ApiSpec {

    private final AdminBrandFacade adminBrandFacade;
    private final AdminProductFacade adminProductFacade;

    // ─────────────────────────────────────────────
    // 브랜드 관리
    // ─────────────────────────────────────────────

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<AdminBrandResponse> createBrand(@RequestBody CreateBrandRequest request) {
        return ApiResponse.success(AdminBrandResponse.from(
            adminBrandFacade.createBrand(request.name(), request.description(), request.logoImageUrl())
        ));
    }

    @PutMapping("/brands/{brandId}")
    @Override
    public ApiResponse<AdminBrandResponse> updateBrand(@PathVariable Long brandId, @RequestBody UpdateBrandRequest request) {
        return ApiResponse.success(AdminBrandResponse.from(
            adminBrandFacade.updateBrand(brandId, request.name(), request.description(), request.logoImageUrl())
        ));
    }

    /**
     * 브랜드를 비활성화(INACTIVE)한다.
     * 연관 상품의 비활성화는 BrandDeactivatedEvent를 통해 비동기로 처리된다.
     */
    @DeleteMapping("/brands/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void deactivateBrand(@PathVariable Long brandId) {
        adminBrandFacade.deactivateBrand(brandId);
    }

    @GetMapping("/brands/{brandId}")
    @Override
    public ApiResponse<AdminBrandResponse> getBrand(@PathVariable Long brandId) {
        // 어드민은 INACTIVE 포함 모든 상태의 브랜드 조회 가능
        return ApiResponse.success(AdminBrandResponse.from(adminBrandFacade.getBrandInfo(brandId)));
    }

    @GetMapping("/brands")
    @Override
    public ApiResponse<PageResponse<AdminBrandResponse>> getBrandList(
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // status 문자열을 BrandStatus enum으로 변환 (null이면 전체 조회)
        BrandStatus brandStatus = status != null ? BrandStatus.valueOf(status) : null;
        return ApiResponse.success(PageResponse.from(
            adminBrandFacade.getBrandList(brandStatus, page, size).map(AdminBrandResponse::from)
        ));
    }

    // ─────────────────────────────────────────────
    // 상품 관리
    // ─────────────────────────────────────────────

    /**
     * 상품을 등록한다.
     * 등록 시 ProductHistory 스냅샷이 1건 자동 생성된다.
     */
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<AdminProductResponse> createProduct(@RequestBody CreateProductRequest request) {
        return ApiResponse.success(AdminProductResponse.from(
            adminProductFacade.createProduct(
                request.brandId(), request.name(), request.price(),
                request.description(), request.thumbnailImageUrl()
            )
        ));
    }

    /**
     * 상품 정보를 수정한다.
     * 수정 시 ProductHistory 스냅샷이 1건 추가된다.
     */
    @PutMapping("/products/{productId}")
    @Override
    public ApiResponse<AdminProductResponse> updateProduct(@PathVariable Long productId, @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(AdminProductResponse.from(
            adminProductFacade.updateProduct(
                productId, request.brandId(), request.name(), request.price(),
                request.description(), request.thumbnailImageUrl()
            )
        ));
    }

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void deactivateProduct(@PathVariable Long productId) {
        adminProductFacade.deactivateProduct(productId);
    }

    @GetMapping("/products/{productId}")
    @Override
    public ApiResponse<AdminProductResponse> getProduct(@PathVariable Long productId) {
        // 어드민은 PENDING/INACTIVE 포함 모든 상태의 상품 조회 가능
        return ApiResponse.success(AdminProductResponse.from(adminProductFacade.getProductInfo(productId)));
    }

    @GetMapping("/products")
    @Override
    public ApiResponse<PageResponse<AdminProductResponse>> getProductList(
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        // status 문자열을 ProductStatus enum으로 변환 (null이면 전체 조회)
        ProductStatus productStatus = status != null ? ProductStatus.valueOf(status) : null;
        return ApiResponse.success(PageResponse.from(
            adminProductFacade.getProductList(brandId, productStatus, page, size).map(AdminProductResponse::from)
        ));
    }

    @GetMapping("/products/{productId}/history")
    @Override
    public ApiResponse<PageResponse<ProductHistoryResponse>> getProductHistory(
        @PathVariable Long productId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ApiResponse.success(PageResponse.from(
            adminProductFacade.getProductHistory(productId, page, size).map(ProductHistoryResponse::from)
        ));
    }
}
