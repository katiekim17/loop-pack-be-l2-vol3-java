package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeListItem;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public class LikeV1Dto {

    // 좋아요 목록 아이템 응답
    public record LikeListItemResponse(
        Long productId,
        String name,
        BrandInfo brand,
        String thumbnailImageUrl,
        Long minPrice,
        long likeCount,
        ZonedDateTime likedAt
    ) {
        public record BrandInfo(Long brandId, String name) {}

        public static LikeListItemResponse from(LikeListItem item) {
            return new LikeListItemResponse(
                item.productId(),
                item.name(),
                new BrandInfo(item.brandId(), item.brandName()),
                item.thumbnailImageUrl(),
                item.minPrice(),
                item.likeCount(),
                item.likedAt()
            );
        }
    }

    // 페이징 응답 래퍼
    public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
            );
        }
    }
}
