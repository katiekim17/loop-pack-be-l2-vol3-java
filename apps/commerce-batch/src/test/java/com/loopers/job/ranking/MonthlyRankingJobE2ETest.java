package com.loopers.job.ranking;

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig;
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
            CREATE TABLE IF NOT EXISTS mv_product_rank_monthly (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                month_start DATE NOT NULL,
                total_sales BIGINT NOT NULL,
                updated_at DATETIME,
                UNIQUE KEY uq_mv_monthly_product_month (product_id, month_start)
            )
            """);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM product_metrics");
        jdbcTemplate.execute("DELETE FROM mv_product_rank_monthly");
    }

    @DisplayName("monthlyRankingJob이 실행되면 product_metrics를 읽어 mv_product_rank_monthly에 집계 결과를 저장한다.")
    @Test
    void savesMonthlyRankingFromProductMetrics() throws Exception {
        // arrange
        jdbcTemplate.execute("INSERT INTO product_metrics (product_id, sales_count) VALUES (1, 300), (2, 150), (3, 500)");
        jobLauncherTestUtils.setJob(job);

        LocalDate targetDate = LocalDate.now();
        LocalDate expectedMonthStart = targetDate.withDayOfMonth(1);

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
                    "SELECT product_id, total_sales FROM mv_product_rank_monthly WHERE month_start = ? ORDER BY total_sales DESC",
                    expectedMonthStart
                );
                assertThat(rows).hasSize(3);
                assertThat(((Number) rows.get(0).get("product_id")).longValue()).isEqualTo(3L);
                assertThat(((Number) rows.get(0).get("total_sales")).longValue()).isEqualTo(500L);
                assertThat(((Number) rows.get(1).get("product_id")).longValue()).isEqualTo(1L);
                assertThat(((Number) rows.get(2).get("product_id")).longValue()).isEqualTo(2L);
            }
        );
    }
}