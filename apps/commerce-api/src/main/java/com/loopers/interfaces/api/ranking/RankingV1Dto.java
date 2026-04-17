package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;

import java.time.LocalDate;
import java.util.List;

public class RankingV1Dto {

    public record RankingPageResponse(
        String periodType,
        LocalDate targetDate,
        List<RankingItemResponse> items
    ) {

        public static RankingPageResponse from(String periodType, LocalDate targetDate, List<RankingInfo> infos) {
            return new RankingPageResponse(
                periodType,
                targetDate,
                infos.stream().map(RankingItemResponse::from).toList()
            );
        }
    }

    public record RankingItemResponse(
        int rank,
        Long productId,
        String productName,
        String brandName,
        long price,
        double score
    ) {
        public static RankingItemResponse from(RankingInfo info) {
            return new RankingItemResponse(
                info.rank(),
                info.productId(),
                info.productName(),
                info.brandName(),
                info.price(),
                info.score()
            );
        }
    }
}
