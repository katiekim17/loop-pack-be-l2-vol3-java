package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
class LikeV1ApiE2ETest {

    private static final String PRODUCTS_ENDPOINT = "/api/v1/products";
    private static final String USERS_ENDPOINT = "/api/v1/users";
    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Test1234!";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    @Autowired
    public LikeV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        DatabaseCleanUp databaseCleanUp,
        BrandJpaRepository brandJpaRepository,
        ProductJpaRepository productJpaRepository
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @BeforeEach
    void setUp() {
        // 테스트마다 유저 회원가입
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

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", LOGIN_ID);
        headers.set("X-Loopers-LoginPw", PASSWORD);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @DisplayName("POST /api/v1/products/{productId}/likes (좋아요 등록)")
    @Nested
    class PostLike {

        @DisplayName("존재하는 상품에 좋아요를 누르면, 201 Created를 반환한다.")
        @Test
        void returnsCreated_whenProductExists() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @DisplayName("같은 상품에 중복 좋아요를 누르면, 409 Conflict를 반환한다.")
        @Test
        void returnsConflict_whenLikeAlreadyExists() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            // act - 두 번째 좋아요 시도
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");

            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("존재하지 않는 상품에 좋아요를 누르면, 404 Not Found를 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // act
            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/999999/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes (좋아요 취소)")
    @Nested
    class DeleteLike {

        @DisplayName("좋아요한 상품을 취소하면, 204 No Content를 반환한다.")
        @Test
        void returnsNoContent_whenLikeExists() {
            // arrange - 먼저 좋아요 등록
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("이미 취소된 좋아요를 재요청해도, 204 No Content를 반환한다 (멱등성).")
        @Test
        void returnsNoContent_whenLikeAlreadyCancelled() {
            // arrange - 좋아요 없이 바로 취소 요청
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class
            );

            // assert - 멱등성: 좋아요가 없어도 성공
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/likes (내 좋아요 목록 조회)")
    @Nested
    class GetMyLikes {

        @DisplayName("좋아요한 상품이 있으면, 200 OK와 목록을 반환한다.")
        @Test
        void returnsLikeList_whenLikesExist() {
            // arrange - 상품 2개 좋아요
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product productA = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명A"));
            Product productB = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(80000L), "설명B"));
            testRestTemplate.exchange(PRODUCTS_ENDPOINT + "/" + productA.getId() + "/likes", HttpMethod.POST, new HttpEntity<>(authHeaders()), new ParameterizedTypeReference<ApiResponse<Void>>() {});
            testRestTemplate.exchange(PRODUCTS_ENDPOINT + "/" + productB.getId() + "/likes", HttpMethod.POST, new HttpEntity<>(authHeaders()), new ParameterizedTypeReference<ApiResponse<Void>>() {});

            // act - userId는 무시되고 헤더 인증 기준으로 조회
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                USERS_ENDPOINT + "/1/likes",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    List<?> content = (List<?>) data.get("content");
                    // 좋아요 2건이 있어야 함
                    assertThat(content).hasSize(2);
                }
            );
        }

        @DisplayName("좋아요가 없으면, 200 OK와 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoLikes() {
            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                USERS_ENDPOINT + "/1/likes",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    List<?> content = (List<?>) response.getBody().data().get("content");
                    assertThat(content).isEmpty();
                }
            );
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        void returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                USERS_ENDPOINT + "/1/likes",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @DisplayName("좋아요 수 비동기 반영")
    @Nested
    class LikeCount {

        @DisplayName("좋아요 등록 후, 상품 상세의 likeCount가 비동기로 1 증가한다.")
        @Test
        void incrementsLikeCount_afterLikeIsAdded() {
            // arrange - 상품 ACTIVE 상태로 생성
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product = productJpaRepository.save(new Product(brand.getId(), "신발", new Money(50000L), "설명"));
            product.activate();
            productJpaRepository.save(product);

            // act - 좋아요 등록
            testRestTemplate.exchange(
                PRODUCTS_ENDPOINT + "/" + product.getId() + "/likes",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            // assert - 비동기 이벤트 처리 완료 대기 후 likeCount 확인
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ResponseEntity<ApiResponse<Map<String, Object>>> detailResponse = testRestTemplate.exchange(
                    PRODUCTS_ENDPOINT + "/" + product.getId(),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<>() {}
                );
                assertThat(((Number) detailResponse.getBody().data().get("likeCount")).longValue()).isEqualTo(1L);
            });
        }
    }
}
