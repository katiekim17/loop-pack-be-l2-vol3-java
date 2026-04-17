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
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRankingE2ETest {

    private static final String PRODUCTS_ENDPOINT = "/api/v1/products";

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

    @DisplayName("GET /api/v1/products/{productId} 의 ranking 필드")
    @Nested
    class ProductDetailRanking {

        @DisplayName("오늘 일간 랭킹에 있는 상품 조회 시 ranking 순위가 반환된다.")
        @Test
        void returnsRankingWhenProductIsRanked() {
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product1 = productJpaRepository.save(new Product(brand.getId(), "1위상품", new Money(50000L), "설명"));
            Product product2 = productJpaRepository.save(new Product(brand.getId(), "2위상품", new Money(30000L), "설명"));

            LocalDate today = LocalDate.now();
            rankingMaterializedViewJpaRepository.save(new RankingMaterializedView("DAILY", today, product1.getId(), 10.0, 1));
            rankingMaterializedViewJpaRepository.save(new RankingMaterializedView("DAILY", today, product2.getId(), 5.0, 2));

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product1.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("ranking")).isNotNull();
                    assertThat(((Number) data.get("ranking")).intValue()).isEqualTo(1);
                }
            );
        }

        @DisplayName("오늘 일간 랭킹에 없는 상품 조회 시 ranking 은 null로 반환된다.")
        @Test
        void returnsNullRankingWhenProductIsNotRanked() {
            Brand brand = brandJpaRepository.save(new Brand("아디다스"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "비인기상품", new Money(20000L), "설명"));

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("ranking")).isNull();
                }
            );
        }
    }
}
