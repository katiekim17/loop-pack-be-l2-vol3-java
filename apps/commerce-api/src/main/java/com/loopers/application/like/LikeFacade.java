package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.users.UserService;
import com.loopers.domain.users.Users;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final UserService userService;
    private final LikeService likeService;
    private final ProductService productService;

    // 좋아요 등록: 인증 → 상품 존재 검증 → 저장 → 비동기 카운트 증가 이벤트
    public void addLike(String loginId, String password, Long productId) {
        Users user = userService.authenticate(loginId, password);
        likeService.addLike(user.getId(), productId);
    }

    // 좋아요 취소: 인증 → 멱등성 보장 삭제 → 비동기 카운트 감소 이벤트
    public void removeLike(String loginId, String password, Long productId) {
        Users user = userService.authenticate(loginId, password);
        likeService.removeLike(user.getId(), productId);
    }

    /**
     * 내 좋아요 목록 조회.
     * - 인증된 사용자의 Like 목록 페이징 조회
     * - 관련 Product / Brand를 배치 로딩하여 N+1 방지
     */
    public Page<LikeListItem> getMyLikes(String loginId, String password, int page, int size) {
        Users user = userService.authenticate(loginId, password);
        Page<Like> likes = likeService.getMyLikes(user.getId(), PageRequest.of(page, size));

        // 배치 로딩
        List<Long> productIds = likes.getContent().stream().map(Like::getProductId).toList();
        if (productIds.isEmpty()) {
            return likes.map(like -> null); // 빈 페이지
        }

        List<Product> products = productService.getProducts(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> brandIds = products.stream().map(Product::getBrandId).distinct().toList();
        List<Brand> brands = productService.getBrands(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));

        return likes.map(like -> {
            Product product = productMap.get(like.getProductId());
            Brand brand = brandMap.get(product.getBrandId());
            return LikeListItem.from(like, product, brand);
        });
    }
}
