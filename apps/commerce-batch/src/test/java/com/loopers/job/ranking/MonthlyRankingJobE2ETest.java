package com.loopers.job.ranking;

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = "spring.batch.job.name=" + MonthlyRankingJobConfig.JOB_NAME)
class MonthlyRankingJobE2ETest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(MonthlyRankingJobConfig.JOB_NAME)
    private Job job;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @DisplayName("monthlyRankingJob이 실행되면 일간 Redis 랭킹을 합산해 ranking_materialized_view에 저장한다.")
    @Test
    void savesMonthlyRankingFromDailyRedisScores() throws Exception {
        jobLauncherTestUtils.setJob(job);

        LocalDate monthStart = LocalDate.of(2026, 4, 1);
        addScore(monthStart, 1L, 4.0);
        addScore(monthStart.plusDays(10), 1L, 3.0);
        addScore(monthStart.plusDays(3), 2L, 10.0);

        var jobExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", "20260417")
                .toJobParameters()
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT product_id, score, rank_no
            FROM ranking_materialized_view
            WHERE period_type = 'MONTHLY' AND target_date = ?
            ORDER BY rank_no ASC
            """,
            monthStart
        );

        assertAll(
            () -> assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode()),
            () -> assertThat(rows).hasSize(2),
            () -> assertThat(((Number) rows.get(0).get("product_id")).longValue()).isEqualTo(2L),
            () -> assertThat(((Number) rows.get(0).get("score")).doubleValue()).isEqualTo(10.0),
            () -> assertThat(((Number) rows.get(0).get("rank_no")).intValue()).isEqualTo(1),
            () -> assertThat(((Number) rows.get(1).get("product_id")).longValue()).isEqualTo(1L),
            () -> assertThat(((Number) rows.get(1).get("score")).doubleValue()).isEqualTo(7.0),
            () -> assertThat(((Number) rows.get(1).get("rank_no")).intValue()).isEqualTo(2)
        );
    }

    private void addScore(LocalDate date, Long productId, double score) {
        String key = "ranking:all:" + date.format(DATE_FORMAT);
        redisTemplate.opsForZSet().add(key, productId.toString(), score);
    }
}
