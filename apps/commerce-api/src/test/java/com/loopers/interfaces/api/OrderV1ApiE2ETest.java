package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.stock.StockJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/orders";
    private static final String USERS_ENDPOINT = "/api/v1/users";
    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Test1234!";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final StockJpaRepository stockJpaRepository;

    @Autowired
    public OrderV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        DatabaseCleanUp databaseCleanUp,
        BrandJpaRepository brandJpaRepository,
        ProductJpaRepository productJpaRepository,
        StockJpaRepository stockJpaRepository
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.stockJpaRepository = stockJpaRepository;
    }

    @BeforeEach
    void setUp() {
        Map<String, String> signUpRequest = Map.of(
            "loginId", LOGIN_ID,
            "password", PASSWORD,
            "name", "홍길동",
            "birthDate", "19900101",
            "email", "test@example.com"
        );
        testRestTemplate.exchange(
            USERS_ENDPOINT,
            HttpMethod.POST,
            new HttpEntity<>(signUpRequest),
            new ParameterizedTypeReference<ApiResponse<Map<String, String>>>() {}
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders (주문 생성)")
    @Nested
    class CreateOrder {

        @DisplayName("모든 재고가 충분하면, 201 Created와 주문 정보를 반환한다.")
        @Test
        void returnsCreated_whenAllStocksAreSufficient() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product productA = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명A"));
            Product productB = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(30000L), "설명B"));
            stockJpaRepository.save(new Stock(productA.getId(), 100L));
            stockJpaRepository.save(new Stock(productB.getId(), 50L));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "items", List.of(
                    Map.of("productId", productA.getId(), "quantity", 2),
                    Map.of("productId", productB.getId(), "quantity", 3)
                )
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data).isNotNull();
                    assertThat(data.get("orderId")).isNotNull();
                    assertThat(data.get("status")).isEqualTo("CREATED");
                    assertThat(((Number) data.get("totalAmount")).longValue())
                        .isEqualTo(50000L * 2 + 30000L * 3); // 190000
                }
            );
        }

        @DisplayName("재고가 부족한 상품이 있으면, 400 Bad Request를 반환한다.")
        @Test
        void returnsBadRequest_whenAnyStockIsInsufficient() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            stockJpaRepository.save(new Stock(product.getId(), 5L)); // 재고 5개

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "items", List.of(
                    Map.of("productId", product.getId(), "quantity", 10) // 재고 초과
                )
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 1L, "quantity", 1))
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 상품 ID로 주문하면, 404 Not Found를 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "items", List.of(Map.of("productId", 999999L, "quantity", 1))
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
