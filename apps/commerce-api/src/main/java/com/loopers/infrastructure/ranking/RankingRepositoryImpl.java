package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingMaterializedView;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingPeriodDateResolver;
import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class RankingRepositoryImpl implements RankingRepository {

    private final RankingMaterializedViewJpaRepository rankingMaterializedViewJpaRepository;

    @Override
    public List<RankedProduct> getTopN(RankingPeriod period, LocalDate date, int page, int size) {
        LocalDate targetDate = RankingPeriodDateResolver.normalize(period.name(), date);
        return rankingMaterializedViewJpaRepository.findAllByPeriodTypeAndTargetDateOrderByRankNoAsc(
                period.name(),
                targetDate,
                PageRequest.of(page - 1, size)
            )
            .stream()
            .map(this::toRankedProduct)
            .toList();
    }

    @Override
    public Optional<Integer> getRank(RankingPeriod period, Long productId, LocalDate date) {
        LocalDate targetDate = RankingPeriodDateResolver.normalize(period.name(), date);
        return rankingMaterializedViewJpaRepository.findByPeriodTypeAndTargetDateAndProductId(
                period.name(),
                targetDate,
                productId
            )
            .map(RankingMaterializedView::getRankNo);
    }

    private RankedProduct toRankedProduct(RankingMaterializedView row) {
        return new RankedProduct(row.getProductId(), row.getScore());
    }
}
