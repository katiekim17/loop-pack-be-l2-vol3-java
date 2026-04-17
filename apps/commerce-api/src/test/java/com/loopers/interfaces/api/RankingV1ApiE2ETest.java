package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.ranking.RankingMaterializedView;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.ranking.RankingMaterializedViewJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/rankings";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private RankingMaterializedViewJpaRepository rankingMaterializedViewJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    private void seedMaterializedView(String periodType, LocalDate targetDate, Long productId, double score, int rankNo) {
        rankingMaterializedViewJpaRepository.save(
            new RankingMaterializedView(periodType, targetDate, productId, score, rankNo)
        );
    }

    @DisplayName("GET /api/v1/rankings")
    @Nested
    class GetRankings {

        @DisplayName("일간 랭킹 조회 시 상품정보가 포함된 순위 목록을 반환한다.")
        @Test
        void returnsDailyRankingWithProductInfo() {
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product1 = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명"));
            Product product2 = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(30000L), "설명"));

            LocalDate today = LocalDate.now();
            seedMaterializedView("DAILY", today, product1.getId(), 5.0, 1);
            seedMaterializedView("DAILY", today, product2.getId(), 3.0, 2);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=DAILY&date=" + today.format(DATE_FORMAT) + "&size=20&page=1",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("periodType")).isEqualTo("DAILY");
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                    assertThat(items).hasSize(2);
                    assertThat(((Number) items.get(0).get("rank")).intValue()).isEqualTo(1);
                    assertThat(items.get(0).get("productName")).isEqualTo("신발A");
                    assertThat(((Number) items.get(1).get("rank")).intValue()).isEqualTo(2);
                    assertThat(items.get(1).get("productName")).isEqualTo("신발B");
                }
            );
        }

        @DisplayName("주간 랭킹 조회 시 기준 날짜를 주 시작일로 정규화해 반환한다.")
        @Test
        void returnsWeeklyRanking() {
            Brand brand = brandJpaRepository.save(new Brand("아디다스"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "운동화", new Money(40000L), "설명"));

            LocalDate requestDate = LocalDate.of(2026, 4, 17);
            LocalDate weekStart = LocalDate.of(2026, 4, 13);
            seedMaterializedView("WEEKLY", weekStart, product.getId(), 10.0, 1);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=WEEKLY&date=" + requestDate.format(DATE_FORMAT),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("periodType")).isEqualTo("WEEKLY");
                    assertThat(data.get("targetDate")).isEqualTo(weekStart.toString());
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                    assertThat(items).hasSize(1);
                    assertThat(items.get(0).get("productName")).isEqualTo("운동화");
                }
            );
        }

        @DisplayName("월간 랭킹 조회 시 기준 날짜를 월 시작일로 정규화해 반환한다.")
        @Test
        void returnsMonthlyRanking() {
            Brand brand = brandJpaRepository.save(new Brand("뉴발란스"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "러닝화", new Money(70000L), "설명"));

            LocalDate requestDate = LocalDate.of(2026, 4, 17);
            LocalDate monthStart = LocalDate.of(2026, 4, 1);
            seedMaterializedView("MONTHLY", monthStart, product.getId(), 15.0, 1);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=MONTHLY&date=" + requestDate.format(DATE_FORMAT),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("periodType")).isEqualTo("MONTHLY");
                    assertThat(data.get("targetDate")).isEqualTo(monthStart.toString());
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                    assertThat(items).hasSize(1);
                    assertThat(items.get(0).get("productName")).isEqualTo("러닝화");
                }
            );
        }

        @DisplayName("랭킹 데이터가 없으면 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoRankingData() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=DAILY&date=" + LocalDate.now().format(DATE_FORMAT),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    List<Object> items = (List<Object>) response.getBody().data().get("items");
                    assertThat(items).isEmpty();
                }
            );
        }

        @DisplayName("주문 1건의 점수가 좋아요 3건보다 높으면 주문 상품이 1위로 반환된다.")
        @Test
        void orderedProductRanksHigherThanThreeLikes() {
            Brand brand = brandJpaRepository.save(new Brand("테스트브랜드"));
            Product likedProduct = productJpaRepository.save(new Product(brand.getId(), "좋아요상품", new Money(10000L), "설명"));
            Product soldProduct = productJpaRepository.save(new Product(brand.getId(), "주문상품", new Money(10000L), "설명"));

            LocalDate today = LocalDate.now();
            seedMaterializedView("DAILY", today, soldProduct.getId(), 0.6 * Math.log1p(10000), 1);
            seedMaterializedView("DAILY", today, likedProduct.getId(), 0.6, 2);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=DAILY&date=" + today.format(DATE_FORMAT) + "&size=10&page=1",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().data().get("items");
            assertThat(items.get(0).get("productName")).isEqualTo("주문상품");
            assertThat(items.get(1).get("productName")).isEqualTo("좋아요상품");
        }

        @DisplayName("페이지 조회 시 size, page 파라미터가 반영된다.")
        @Test
        void respectsPageAndSizeParams() {
            Brand brand = brandJpaRepository.save(new Brand("페이지테스트"));
            LocalDate today = LocalDate.now();
            for (int i = 1; i <= 5; i++) {
                Product product = productJpaRepository.save(new Product(brand.getId(), "상품" + i, new Money(10000L), "설명"));
                seedMaterializedView("DAILY", today, product.getId(), 6.0 - i, i);
            }

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?periodType=DAILY&date=" + today.format(DATE_FORMAT) + "&size=2&page=2",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().data().get("items");
            assertThat(items).hasSize(2);
            assertThat(((Number) items.get(0).get("rank")).intValue()).isEqualTo(3);
            assertThat(((Number) items.get(1).get("rank")).intValue()).isEqualTo(4);
        }
    }
}
