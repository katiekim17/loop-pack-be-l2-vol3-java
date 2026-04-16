package com.loopers.job.ranking;

import com.loopers.batch.job.ranking.WeeklyRankingJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.DayOfWeek;
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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS product_metrics (
                product_id BIGINT PRIMARY KEY,
                like_count BIGINT NOT NULL DEFAULT 0,
                sales_count BIGINT NOT NULL DEFAULT 0,
                view_count BIGINT NOT NULL DEFAULT 0,
                updated_at DATETIME
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS mv_product_rank_weekly (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                week_start DATE NOT NULL,
                total_sales BIGINT NOT NULL,
                updated_at DATETIME,
                UNIQUE KEY uq_mv_weekly_product_week (product_id, week_start)
            )
            """);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM product_metrics");
        jdbcTemplate.execute("DELETE FROM mv_product_rank_weekly");
    }

    @DisplayName("weeklyRankingJob이 실행되면 product_metrics를 읽어 mv_product_rank_weekly에 집계 결과를 저장한다.")
    @Test
    void savesWeeklyRankingFromProductMetrics() throws Exception {
        // arrange
        jdbcTemplate.execute("INSERT INTO product_metrics (product_id, sales_count) VALUES (1, 100), (2, 50), (3, 200)");
        jobLauncherTestUtils.setJob(job);

        LocalDate targetDate = LocalDate.now();
        LocalDate expectedWeekStart = targetDate.with(DayOfWeek.MONDAY);

        // act
        var jobExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", targetDate.format(DATE_FORMAT))
                .toJobParameters()
        );

        // assert
        assertAll(
            () -> assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode()),
            () -> {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT product_id, total_sales FROM mv_product_rank_weekly WHERE week_start = ? ORDER BY total_sales DESC",
                    expectedWeekStart
                );
                assertThat(rows).hasSize(3);
                assertThat(((Number) rows.get(0).get("product_id")).longValue()).isEqualTo(3L);
                assertThat(((Number) rows.get(0).get("total_sales")).longValue()).isEqualTo(200L);
                assertThat(((Number) rows.get(1).get("product_id")).longValue()).isEqualTo(1L);
                assertThat(((Number) rows.get(2).get("product_id")).longValue()).isEqualTo(2L);
            }
        );
    }

    @DisplayName("weeklyRankingJob을 재실행하면 기존 데이터를 덮어쓴다.")
    @Test
    void overwritesExistingRankingOnRerun() throws Exception {
        // arrange
        jdbcTemplate.execute("INSERT INTO product_metrics (product_id, sales_count) VALUES (1, 100)");
        jobLauncherTestUtils.setJob(job);

        String targetDate = LocalDate.now().format(DATE_FORMAT);

        // act — 첫 번째 실행
        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addLong("run.id", 1L)
                .toJobParameters()
        );

        // product_metrics 업데이트 후 재실행 (run.id로 새 JobInstance 생성)
        jdbcTemplate.execute("UPDATE product_metrics SET sales_count = 999 WHERE product_id = 1");
        var secondExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addLong("run.id", 2L)
                .toJobParameters()
        );

        // assert — 최신 값으로 덮어씌워짐
        assertThat(secondExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());
        Long totalSales = jdbcTemplate.queryForObject(
            "SELECT total_sales FROM mv_product_rank_weekly WHERE product_id = 1 AND week_start = ?",
            Long.class,
            LocalDate.now().with(DayOfWeek.MONDAY)
        );
        assertThat(totalSales).isEqualTo(999L);
    }
}
