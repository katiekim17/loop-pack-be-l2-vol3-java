package com.loopers.interfaces.api.admin;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.admin.AdminCouponV1Dto.AdminCouponResponse;
import com.loopers.interfaces.api.admin.AdminCouponV1Dto.AdminUserCouponResponse;
import com.loopers.interfaces.api.admin.AdminCouponV1Dto.CreateCouponRequest;
import com.loopers.interfaces.api.admin.AdminCouponV1Dto.PageResponse;
import com.loopers.interfaces.api.admin.AdminCouponV1Dto.UpdateCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Admin Coupon API", description = "어드민 쿠폰 관리 API")
public interface AdminCouponApiSpec {

    @Operation(summary = "쿠폰 템플릿 등록", description = "새 쿠폰 템플릿을 등록한다.")
    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AdminCouponResponse> createCoupon(@RequestBody CreateCouponRequest request);

    @Operation(summary = "쿠폰 템플릿 수정", description = "이름, 최소 주문 금액, 만료 일시를 수정한다.")
    @PutMapping("/coupons/{couponId}")
    ApiResponse<AdminCouponResponse> updateCoupon(@PathVariable Long couponId, @RequestBody UpdateCouponRequest request);

    @Operation(summary = "쿠폰 템플릿 삭제", description = "쿠폰 템플릿을 soft delete 처리한다.")
    @DeleteMapping("/coupons/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCoupon(@PathVariable Long couponId);

    @Operation(summary = "쿠폰 템플릿 상세 조회", description = "쿠폰 템플릿 단건 조회.")
    @GetMapping("/coupons/{couponId}")
    ApiResponse<AdminCouponResponse> getCoupon(@PathVariable Long couponId);

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "삭제되지 않은 쿠폰 목록을 페이징 조회한다.")
    @GetMapping("/coupons")
    ApiResponse<PageResponse<AdminCouponResponse>> getCoupons(
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );

    @Operation(summary = "쿠폰 발급 내역 조회", description = "특정 쿠폰에 대한 발급 내역을 발급 일시 내림차순으로 조회한다.")
    @GetMapping("/coupons/{couponId}/issues")
    ApiResponse<PageResponse<AdminUserCouponResponse>> getIssuedCoupons(
        @PathVariable Long couponId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );
}
