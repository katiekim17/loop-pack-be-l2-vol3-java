package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Order V1 API", description = "주문 API")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "상품 목록을 받아 주문을 생성한다."
    )
    ApiResponse<OrderV1Dto.CreateOrderResponse> createOrder(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        OrderV1Dto.CreateOrderRequest request
    );
}
