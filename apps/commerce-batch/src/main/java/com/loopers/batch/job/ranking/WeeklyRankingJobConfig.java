package com.loopers.batch.job.ranking;

import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = WeeklyRankingJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class WeeklyRankingJobConfig {

    public static final String JOB_NAME = "weeklyRankingJob";
    private static final String STEP_NAME = "weeklyRankingStep";
    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobRepository jobRepository;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final PlatformTransactionManager transactionManager;

    @Bean(JOB_NAME)
    public Job weeklyRankingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(weeklyRankingStep())
            .listener(jobListener)
            .build();
    }

    @JobScope
    @Bean(STEP_NAME)
    public Step weeklyRankingStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<ProductMetricsRow, MvWeeklyRow>chunk(CHUNK_SIZE, transactionManager)
            .reader(weeklyRankingReader(null))
            .processor(weeklyRankingProcessor(null))
            .writer(weeklyRankingWriter(null))
            .listener(stepMonitorListener)
            .build();
    }

    @StepScope
    @Bean
    public JdbcPagingItemReader<ProductMetricsRow> weeklyRankingReader(DataSource dataSource) {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT product_id, sales_count");
        queryProvider.setFromClause("FROM product_metrics");
        queryProvider.setSortKeys(Map.of(
            "sales_count", Order.DESCENDING,
            "product_id", Order.ASCENDING
        ));

        return new JdbcPagingItemReaderBuilder<ProductMetricsRow>()
            .name("weeklyRankingReader")
            .dataSource(dataSource)
            .queryProvider(queryProvider)
            .rowMapper((rs, rowNum) -> new ProductMetricsRow(
                rs.getLong("product_id"),
                rs.getLong("sales_count")
            ))
            .pageSize(CHUNK_SIZE)
            .maxItemCount(TOP_N)
            .build();
    }

    @StepScope
    @Bean
    public ItemProcessor<ProductMetricsRow, MvWeeklyRow> weeklyRankingProcessor(
        @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        LocalDate weekStart = resolveDate(targetDate).with(DayOfWeek.MONDAY);
        return item -> new MvWeeklyRow(item.productId(), weekStart, item.salesCount());
    }

    @StepScope
    @Bean
    public JdbcBatchItemWriter<MvWeeklyRow> weeklyRankingWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MvWeeklyRow>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO mv_product_rank_weekly (product_id, week_start, total_sales, updated_at)
                VALUES (:productId, :weekStart, :totalSales, NOW())
                ON DUPLICATE KEY UPDATE total_sales = VALUES(total_sales), updated_at = NOW()
                """)
            .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                .addValue("productId", item.productId())
                .addValue("weekStart", item.weekStart())
                .addValue("totalSales", item.totalSales()))
            .build();
    }

    private LocalDate resolveDate(String targetDate) {
        if (targetDate == null || targetDate.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(targetDate, DATE_FORMAT);
    }

    record ProductMetricsRow(Long productId, Long salesCount) {}

    record MvWeeklyRow(Long productId, LocalDate weekStart, Long totalSales) {}
}
