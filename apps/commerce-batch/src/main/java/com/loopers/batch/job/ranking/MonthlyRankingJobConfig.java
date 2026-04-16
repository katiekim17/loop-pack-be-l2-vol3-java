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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = MonthlyRankingJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class MonthlyRankingJobConfig {

    public static final String JOB_NAME = "monthlyRankingJob";
    private static final String STEP_NAME = "monthlyRankingStep";
    private static final int CHUNK_SIZE = 100;
    private static final int TOP_N = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobRepository jobRepository;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final PlatformTransactionManager transactionManager;

    @Bean(JOB_NAME)
    public Job monthlyRankingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(monthlyRankingStep())
            .listener(jobListener)
            .build();
    }

    @JobScope
    @Bean(STEP_NAME)
    public Step monthlyRankingStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<ProductMetricsRow, MvMonthlyRow>chunk(CHUNK_SIZE, transactionManager)
            .reader(monthlyRankingReader(null))
            .processor(monthlyRankingProcessor(null))
            .writer(monthlyRankingWriter(null))
            .listener(stepMonitorListener)
            .build();
    }

    @StepScope
    @Bean
    public JdbcPagingItemReader<ProductMetricsRow> monthlyRankingReader(DataSource dataSource) {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT product_id, sales_count");
        queryProvider.setFromClause("FROM product_metrics");
        queryProvider.setSortKeys(Map.of(
            "sales_count", Order.DESCENDING,
            "product_id", Order.ASCENDING
        ));

        return new JdbcPagingItemReaderBuilder<ProductMetricsRow>()
            .name("monthlyRankingReader")
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
    public ItemProcessor<ProductMetricsRow, MvMonthlyRow> monthlyRankingProcessor(
        @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        LocalDate monthStart = resolveDate(targetDate).withDayOfMonth(1);
        return item -> new MvMonthlyRow(item.productId(), monthStart, item.salesCount());
    }

    @StepScope
    @Bean
    public JdbcBatchItemWriter<MvMonthlyRow> monthlyRankingWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MvMonthlyRow>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO mv_product_rank_monthly (product_id, month_start, total_sales, updated_at)
                VALUES (:productId, :monthStart, :totalSales, NOW())
                ON DUPLICATE KEY UPDATE total_sales = VALUES(total_sales), updated_at = NOW()
                """)
            .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                .addValue("productId", item.productId())
                .addValue("monthStart", item.monthStart())
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

    record MvMonthlyRow(Long productId, LocalDate monthStart, Long totalSales) {}
}
