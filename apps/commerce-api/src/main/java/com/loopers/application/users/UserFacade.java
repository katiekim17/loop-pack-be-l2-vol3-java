package com.loopers.application.users;

import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import com.loopers.interfaces.api.users.UserV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Facade는 “조합/변환”만 한다는 원칙에 맞게!
@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo signupUser(UserV1Dto.SignUpRequest request) {
        Users saved = userService.register(
            request.loginId(), request.password(),
            request.name(), request.birthDate(), request.email()
        );
        return UserInfo.from(saved);
    }

    public UserInfo getMyInfo(String loginId, String password) {
        Users users = userService.authenticate(loginId, password);
        return UserInfo.from(users);
    }

    public void changePassword(String loginId, String password, String prevPassword, String newPassword) {
        userService.authenticate(loginId, password);
        userService.changePassword(loginId, prevPassword, newPassword);
    }
}