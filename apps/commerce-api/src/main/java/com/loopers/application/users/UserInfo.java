package com.loopers.application.users;

import com.loopers.domain.users.Users;

// MemberInfo는 Facade → Controller로 전달되는 데이터
public record UserInfo(String loginId, String name, String birthDate, String email) {
    public static UserInfo from(Users model) {
        return new UserInfo(
            model.getLoginId(),
            model.getMaskedName(),
            model.getBirthDate(),
            model.getEmail()
          );
    }
}
