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
class PaymentV1ApiE2ETest {

    private static final String PAYMENT_ENDPOINT = "/api/v1/payments";
    private static final String ORDERS_ENDPOINT = "/api/v1/orders";
    private static final String USERS_ENDPOINT = "/api/v1/users";
    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Test1234!";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final StockJpaRepository stockJpaRepository;

    @Autowired
    public PaymentV1ApiE2ETest(
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

    private Long createOrder() {
        Brand brand = brandJpaRepository.save(new Brand("나이키"));
        Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
        stockJpaRepository.save(new Stock(product.getId(), 100L));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", LOGIN_ID);
        headers.set("X-Loopers-LoginPw", PASSWORD);
        headers.set("Content-Type", "application/json");

        Map<String, Object> request = Map.of(
            "items", List.of(Map.of("productId", product.getId(), "quantity", 2))
        );

        ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
            ORDERS_ENDPOINT,
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {}
        );

        return ((Number) response.getBody().data().get("orderId")).longValue();
    }

    @DisplayName("POST /api/v1/payments (결제)")
    @Nested
    class Pay {

        @DisplayName("유효한 주문과 카드 정보로 결제하면, 201 Created와 결제 정보를 반환한다.")
        @Test
        void returnsCreated_whenPaymentIsSuccessful() {
            // arrange
            Long orderId = createOrder();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "orderId", orderId,
                "cardType", "SAMSUNG",
                "cardNo", "1234-5678-9814-1451"
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("paymentId")).isNotNull();
                    assertThat(data.get("status")).isEqualTo("COMPLETED");
                    assertThat(((Number) data.get("amount")).longValue()).isEqualTo(100000L);
                }
            );
        }

        @DisplayName("잘못된 인증 정보로 결제하면, 401 Unauthorized를 반환한다.")
        @Test
        void returnsUnauthorized_whenAuthIsInvalid() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "orderId", 1L,
                "cardType", "SAMSUNG",
                "cardNo", "1234-5678-9814-1451"
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 주문 ID로 결제하면, 404 Not Found를 반환한다.")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "orderId", 999999L,
                "cardType", "SAMSUNG",
                "cardNo", "1234-5678-9814-1451"
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("이미 결제된 주문에 재결제 요청하면, 409 Conflict를 반환한다.")
        @Test
        void returnsConflict_whenOrderAlreadyPaid() {
            // arrange
            Long orderId = createOrder();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", PASSWORD);
            headers.set("Content-Type", "application/json");

            Map<String, Object> request = Map.of(
                "orderId", orderId,
                "cardType", "SAMSUNG",
                "cardNo", "1234-5678-9814-1451"
            );

            // 첫 번째 결제
            testRestTemplate.exchange(
                PAYMENT_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            // act - 두 번째 결제 시도
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }
}