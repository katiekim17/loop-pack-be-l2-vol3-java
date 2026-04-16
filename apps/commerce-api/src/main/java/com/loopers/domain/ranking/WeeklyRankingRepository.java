package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyRankingRepository {

    /**
     * 주간 MV에서 상위 N개의 상품 ID와 점수를 반환한다 (total_sales 내림차순).
     * weekStart는 해당 주의 월요일. page는 1-based.
     */
    List<RankingRepository.RankedProduct> getTopN(LocalDate weekStart, int page, int size);
}
