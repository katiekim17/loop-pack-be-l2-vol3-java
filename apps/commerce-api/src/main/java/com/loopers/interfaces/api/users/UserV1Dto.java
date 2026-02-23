package com.loopers.interfaces.api.users;


import com.loopers.application.users.UserInfo;

public class UserV1Dto {

    // Request: POST방식으로 보낼때 데이터를 담는 그릇 (from 필요 없음)
    public record SignUpRequest(
        String loginId,
        String password,
        String name,
        String birthDate,
        String email
    ) {}

    // Response: 변환 메서드(from)가 여기에!
    public record SignUpResponse(String loginId) {
      public static SignUpResponse from(UserInfo info) {
        return new SignUpResponse(info.loginId());
      }
    }

    public record UserInfoResponse(
        String loginId,
        String name,
        String birthDate,
        String email
    ) {
      public static UserInfoResponse from(UserInfo info) {
        return new UserInfoResponse(
            info.loginId(),
            info.name(),
            info.birthDate(),
            info.email()
        );
      }
    }


    // Request: 비밀번호 변경 요청
    public record ChangePasswordRequest(
      String oldPassword,
      String newPassword
    ) {}


}
