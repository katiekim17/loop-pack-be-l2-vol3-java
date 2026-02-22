package com.loopers.interfaces.api.users;

import com.loopers.application.users.UserFacade;
import com.loopers.application.users.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.users.UserV1Dto.ChangePasswordRequest;
import com.loopers.interfaces.api.users.UserV1Dto.MemberInfoResponse;
import com.loopers.interfaces.api.users.UserV1Dto.SignUpRequest;
import com.loopers.interfaces.api.users.UserV1Dto.SignUpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

  //  클라이언트 → [SignUpRequest (record)] → Controller → Facade → Service → [MemberModel (entity)] → DB
  //                   요청 데이터 전달용                                           DB에 저장되는 객체
  //  DB → [MemberModel (entity)] → Facade → [SignUpResponse (record)] → Controller → 클라이언트
  //            DB에서 꺼낸 객체                     응답 데이터 전달용
  private final UserFacade userFacade;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  public ApiResponse<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
    UserInfo info = userFacade.signupUser(request);
    UserV1Dto.SignUpResponse response = UserV1Dto.SignUpResponse.from(info);
    return ApiResponse.success(response);
  }

  @GetMapping("/me")
  @Override
  public ApiResponse<MemberInfoResponse> getMyInfo(
      @RequestHeader("X-Loopers-LoginId") String loginId,
      @RequestHeader("X-Loopers-LoginPw") String password
  ) {
    UserInfo info = userFacade.getMyInfo(loginId, password);
    MemberInfoResponse response = MemberInfoResponse.from(info);
    return ApiResponse.success(response);
  }

  @PatchMapping("/password")
  @Override
  public ApiResponse<String> changePassword(
      @RequestHeader("X-Loopers-LoginId") String loginId,
      @RequestHeader("X-Loopers-LoginPw") String password,
      @RequestBody ChangePasswordRequest request
  ) {
    userFacade.changePassword(loginId, password, request.oldPassword(), request.newPassword());
    return ApiResponse.success("비밀번호가 변경되었습니다.");
  }

  @GetMapping("me/likes")
  @Override
  public ApiResponse<MemberInfoResponse> getMyLikes(
      @RequestHeader("X-Loopers-LoginId") String loginId,
      @RequestHeader("X-Loopers-LoginPw") String password
  ) {
    UserInfo info = userFacade.getMyInfo(loginId, password);
    MemberInfoResponse response = MemberInfoResponse.from(info);
    return ApiResponse.success(response);
  }

}
