package com.loopers.interfaces.api.order;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderV1ApiSpec {

//  @PostMapping
//  @ResponseStatus(HttpStatus.CREATED)
//  @Override
//  public ApiResponse<SignUpResponse> makeOrder(@RequestBody SignUpRequest request) {
//    UserInfo info = userFacade.signupUser(request);
//    UserV1Dto.SignUpResponse response = UserV1Dto.SignUpResponse.from(info);
//    return ApiResponse.success(response);
//  }
//
//  @GetMapping("/startAt=2026-01-31&endAt=2026-02-10")
//  @Override
//  public ApiResponse<MemberInfoResponse> getMyOrders(
//      @RequestHeader("X-Loopers-LoginId") String loginId,
//      @RequestHeader("X-Loopers-LoginPw") String password
//  ) {
//    UserInfo info = userFacade.getMyInfo(loginId, password);
//    MemberInfoResponse response = MemberInfoResponse.from(info);
//    return ApiResponse.success(response);
//  }
//
//  @GetMapping("/{orderId}")
//  @Override
//  public ApiResponse<MemberInfoResponse> getOrder(
//      @RequestHeader("X-Loopers-LoginId") String loginId,
//      @RequestHeader("X-Loopers-LoginPw") String password
//  ) {
//    UserInfo info = userFacade.getMyInfo(loginId, password);
//    MemberInfoResponse response = MemberInfoResponse.from(info);
//    return ApiResponse.success(response);
//  }
}
