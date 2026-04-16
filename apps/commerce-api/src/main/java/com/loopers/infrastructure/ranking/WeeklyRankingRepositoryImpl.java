package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingRepository.RankedProduct;
import com.loopers.domain.ranking.WeeklyRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class WeeklyRankingRepositoryImpl implements WeeklyRankingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<RankedProduct> getTopN(LocalDate weekStart, int page, int size) {
        long offset = (long) (page - 1) * size;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("weekStart", weekStart)
            .addValue("limit", size)
            .addValue("offset", offset);

        return jdbcTemplate.query(
            """
                SELECT product_id, total_sales
                FROM mv_product_rank_weekly
                WHERE week_start = :weekStart
                ORDER BY total_sales DESC, product_id ASC
                LIMIT :limit OFFSET :offset
                """,
            params,
            (rs, rowNum) -> new RankedProduct(rs.getLong("product_id"), rs.getDouble("total_sales"))
        );
    }
}
