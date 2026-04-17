package com.loopers.job.ranking;

import com.loopers.batch.job.ranking.WeeklyRankingJobConfig;
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
@TestPropertySource(properties = "spring.batch.job.name=" + WeeklyRankingJobConfig.JOB_NAME)
class WeeklyRankingJobE2ETest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(WeeklyRankingJobConfig.JOB_NAME)
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

    @DisplayName("weeklyRankingJob이 실행되면 일간 Redis 랭킹을 합산해 ranking_materialized_view에 저장한다.")
    @Test
    void savesWeeklyRankingFromDailyRedisScores() throws Exception {
        jobLauncherTestUtils.setJob(job);

        LocalDate monday = LocalDate.of(2026, 4, 13);
        addScore(monday, 1L, 3.0);
        addScore(monday.plusDays(1), 1L, 2.0);
        addScore(monday.plusDays(2), 2L, 6.0);

        var jobExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", "20260417")
                .toJobParameters()
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            SELECT product_id, score, rank_no
            FROM ranking_materialized_view
            WHERE period_type = 'WEEKLY' AND target_date = ?
            ORDER BY rank_no ASC
            """,
            monday
        );

        assertAll(
            () -> assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode()),
            () -> assertThat(rows).hasSize(2),
            () -> assertThat(((Number) rows.get(0).get("product_id")).longValue()).isEqualTo(2L),
            () -> assertThat(((Number) rows.get(0).get("score")).doubleValue()).isEqualTo(6.0),
            () -> assertThat(((Number) rows.get(0).get("rank_no")).intValue()).isEqualTo(1),
            () -> assertThat(((Number) rows.get(1).get("product_id")).longValue()).isEqualTo(1L),
            () -> assertThat(((Number) rows.get(1).get("score")).doubleValue()).isEqualTo(5.0),
            () -> assertThat(((Number) rows.get(1).get("rank_no")).intValue()).isEqualTo(2)
        );
    }

    @DisplayName("weeklyRankingJob을 재실행하면 같은 주간 bucket 데이터를 새 점수로 덮어쓴다.")
    @Test
    void overwritesExistingWeeklyRowsOnRerun() throws Exception {
        jobLauncherTestUtils.setJob(job);

        LocalDate monday = LocalDate.of(2026, 4, 13);
        addScore(monday, 1L, 1.0);

        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", "20260417")
                .addLong("run.id", 1L)
                .toJobParameters()
        );

        redisCleanUp.truncateAll();
        addScore(monday, 1L, 7.0);

        var secondExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", "20260417")
                .addLong("run.id", 2L)
                .toJobParameters()
        );

        Double score = jdbcTemplate.queryForObject(
            """
            SELECT score
            FROM ranking_materialized_view
            WHERE period_type = 'WEEKLY' AND target_date = ? AND product_id = 1
            """,
            Double.class,
            monday
        );

        assertAll(
            () -> assertThat(secondExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode()),
            () -> assertThat(score).isEqualTo(7.0)
        );
    }

    private void addScore(LocalDate date, Long productId, double score) {
        String key = "ranking:all:" + date.format(DATE_FORMAT);
        redisTemplate.opsForZSet().add(key, productId.toString(), score);
    }
}
