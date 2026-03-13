package com.loopers.interfaces.api.users;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.users.UserFacade;
import com.loopers.application.users.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeV1Dto;
import com.loopers.interfaces.api.users.UserV1Dto.ChangePasswordRequest;
import com.loopers.interfaces.api.users.UserV1Dto.UserInfoResponse;
import com.loopers.interfaces.api.users.UserV1Dto.SignUpRequest;
import com.loopers.interfaces.api.users.UserV1Dto.SignUpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;
    private final LikeFacade likeFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        UserInfo info = userFacade.signupUser(request);
        return ApiResponse.success(SignUpResponse.from(info));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserInfoResponse> getMyInfo(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password
    ) {
        UserInfo info = userFacade.getMyInfo(loginId, password);
        return ApiResponse.success(UserInfoResponse.from(info));
    }

    @PatchMapping("/me/password")
    @Override
    public ApiResponse<String> changePassword(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @RequestBody ChangePasswordRequest request
    ) {
        userFacade.changePassword(loginId, password, request.oldPassword(), request.newPassword());
        return ApiResponse.success("비밀번호가 변경되었습니다.");
    }

    /**
     * 내 좋아요 목록 조회.
     * URL의 {userId}는 향후 확장을 위한 구조이며, 현재는 헤더 인증 기준으로 본인 목록만 반환한다.
     */
    @GetMapping("/{userId}/likes")
    @Override
    public ApiResponse<LikeV1Dto.PageResponse<LikeV1Dto.LikeListItemResponse>> getMyLikes(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long userId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ApiResponse.success(LikeV1Dto.PageResponse.from(
            likeFacade.getMyLikes(loginId, password, page, size)
                .map(LikeV1Dto.LikeListItemResponse::from)
        ));
    }
}
