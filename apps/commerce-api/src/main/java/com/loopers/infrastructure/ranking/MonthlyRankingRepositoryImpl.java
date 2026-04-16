package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyRankingRepository;
import com.loopers.domain.ranking.RankingRepository.RankedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class MonthlyRankingRepositoryImpl implements MonthlyRankingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<RankedProduct> getTopN(LocalDate monthStart, int page, int size) {
        long offset = (long) (page - 1) * size;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("monthStart", monthStart)
            .addValue("limit", size)
            .addValue("offset", offset);

        return jdbcTemplate.query(
            """
                SELECT product_id, total_sales
                FROM mv_product_rank_monthly
                WHERE month_start = :monthStart
                ORDER BY total_sales DESC, product_id ASC
                LIMIT :limit OFFSET :offset
                """,
            params,
            (rs, rowNum) -> new RankedProduct(rs.getLong("product_id"), rs.getDouble("total_sales"))
        );
    }
}