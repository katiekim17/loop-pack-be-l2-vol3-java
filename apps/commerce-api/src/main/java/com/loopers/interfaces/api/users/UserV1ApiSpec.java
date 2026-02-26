package com.loopers.interfaces.api.users;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Member V1 API", description = "회원 API")
public interface UserV1ApiSpec {

    @Operation(summary = "회원 가입 요청", description = "주어진 정보를 가지고 회원 가입을 실행한다")
    ApiResponse<UserV1Dto.SignUpResponse> signUp(UserV1Dto.SignUpRequest request);

    @Operation(summary = "내 정보 조회", description = "로그인 ID로 내 회원 정보를 조회한다")
    ApiResponse<UserV1Dto.UserInfoResponse> getMyInfo(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password
    );

    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호와 새 비밀번호를 받아 비밀번호를 변경한다")
    ApiResponse<String> changePassword(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        UserV1Dto.ChangePasswordRequest request
    );

    @Operation(summary = "내 좋아요 목록 조회", description = "인증된 사용자의 좋아요 목록을 반환한다. URL의 userId는 무시된다.")
    ApiResponse<LikeV1Dto.PageResponse<LikeV1Dto.LikeListItemResponse>> getMyLikes(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long userId,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    );
}
