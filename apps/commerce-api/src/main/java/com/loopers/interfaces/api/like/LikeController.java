package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좋아요 등록/취소 API.
 * ProductsV1Controller와 같은 /api/v1/products 기반 경로를 사용하지만
 * HTTP 메서드(POST/DELETE)가 다르므로 충돌하지 않는다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class LikeController implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/{productId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<Void> addLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long productId
    ) {
        likeFacade.addLike(loginId, password, productId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{productId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void removeLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long productId
    ) {
        likeFacade.removeLike(loginId, password, productId);
    }
}
