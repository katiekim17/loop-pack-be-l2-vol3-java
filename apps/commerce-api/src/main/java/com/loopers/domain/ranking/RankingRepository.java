package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RankingRepository {

    List<RankedProduct> getTopN(RankingPeriod period, LocalDate date, int page, int size);

    Optional<Integer> getRank(RankingPeriod period, Long productId, LocalDate date);

    record RankedProduct(Long productId, double score) {}
}
