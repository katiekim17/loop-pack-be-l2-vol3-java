package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Like V1 API", description = "좋아요 API")
public interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록한다. 중복 시 409.")
    @PostMapping("/{productId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Void> addLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long productId
    );

    @Operation(summary = "좋아요 취소", description = "좋아요를 취소한다. 이미 취소된 경우에도 성공 응답 (멱등성).")
    @DeleteMapping("/{productId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long productId
    );
}
