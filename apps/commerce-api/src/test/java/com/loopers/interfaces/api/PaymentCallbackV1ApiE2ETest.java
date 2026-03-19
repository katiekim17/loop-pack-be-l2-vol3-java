package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.ExternalPaymentClient;
import com.loopers.domain.payment.ExternalPaymentResponse;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentCallbackV1ApiE2ETest {

    private static final String CALLBACK_ENDPOINT = "/api/v1/payments/callback";
    private static final String PAYMENT_ENDPOINT = "/api/v1/payments";
    private static final String ORDERS_ENDPOINT = "/api/v1/orders";
    private static final String USERS_ENDPOINT = "/api/v1/users";
    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Test1234!";

    @MockBean
    private ExternalPaymentClient externalPaymentClient;

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final StockJpaRepository stockJpaRepository;

    @Autowired
    public PaymentCallbackV1ApiE2ETest(
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

    private Long createPendingPayment() {
        Brand brand = brandJpaRepository.save(new Brand("나이키"));
        Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
        stockJpaRepository.save(new Stock(product.getId(), 100L));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", LOGIN_ID);
        headers.set("X-Loopers-LoginPw", PASSWORD);
        headers.set("Content-Type", "application/json");

        Map<String, Object> orderRequest = Map.of(
            "items", List.of(Map.of("productId", product.getId(), "quantity", 2))
        );

        ResponseEntity<ApiResponse<Map<String, Object>>> orderResponse = testRestTemplate.exchange(
            ORDERS_ENDPOINT,
            HttpMethod.POST,
            new HttpEntity<>(orderRequest, headers),
            new ParameterizedTypeReference<>() {}
        );
        Long orderId = ((Number) orderResponse.getBody().data().get("orderId")).longValue();

        // Simulate timeout → ExternalPaymentClient throws exception → PENDING saved
        when(externalPaymentClient.pay(anyLong(), any(CardType.class), anyString(), anyLong()))
            .thenThrow(new RuntimeException("Connection timed out"));

        Map<String, Object> payRequest = Map.of(
            "orderId", orderId,
            "cardType", "SAMSUNG",
            "cardNo", "1234-5678-9814-1451"
        );

        testRestTemplate.exchange(
            PAYMENT_ENDPOINT,
            HttpMethod.POST,
            new HttpEntity<>(payRequest, headers),
            new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
        );

        return orderId;
    }

    @DisplayName("POST /api/v1/payments/callback (결제 콜백)")
    @Nested
    class Callback {

        @DisplayName("success=true 콜백이 오면, PENDING 결제를 COMPLETED 로 업데이트하고 200 OK 를 반환한다.")
        @Test
        void updatesToCompleted_whenCallbackSucceeds() {
            // arrange
            Long orderId = createPendingPayment();
            String transactionId = "txn-callback-001";

            Map<String, Object> request = Map.of(
                "transactionId", transactionId,
                "orderId", orderId,
                "success", true
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                CALLBACK_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("status")).isEqualTo("COMPLETED")
            );
        }

        @DisplayName("success=false 콜백이 오면, PENDING 결제를 FAILED 로 업데이트하고 200 OK 를 반환한다.")
        @Test
        void updatesToFailed_whenCallbackFails() {
            // arrange
            Long orderId = createPendingPayment();

            Map<String, Object> request = Map.of(
                "transactionId", "txn-failed",
                "orderId", orderId,
                "success", false
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                CALLBACK_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("status")).isEqualTo("FAILED")
            );
        }

        @DisplayName("존재하지 않는 orderId 로 콜백이 오면, 404 Not Found 를 반환한다.")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            Map<String, Object> request = Map.of(
                "transactionId", "txn-001",
                "orderId", 999999L,
                "success", true
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                CALLBACK_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
