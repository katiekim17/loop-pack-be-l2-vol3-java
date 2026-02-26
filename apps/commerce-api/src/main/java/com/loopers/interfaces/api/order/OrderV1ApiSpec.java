package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order V1 API", description = "주문 API")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "상품 목록을 받아 주문을 생성한다.")
    ApiResponse<OrderV1Dto.CreateOrderResponse> createOrder(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        OrderV1Dto.CreateOrderRequest request
    );

    @Operation(summary = "유저 주문 목록 조회", description = "인증된 사용자의 주문 목록을 최신순으로 반환한다.")
    ApiResponse<OrderV1Dto.PageResponse<OrderV1Dto.OrderSummaryResponse>> getOrderList(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );

    @Operation(summary = "주문 단건 상세 조회", description = "주문 ID로 주문 상세 및 아이템 목록을 반환한다.")
    ApiResponse<OrderV1Dto.OrderDetailResponse> getOrderDetail(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long orderId
    );
}
