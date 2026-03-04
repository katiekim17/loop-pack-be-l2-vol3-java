package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.ZonedDateTime;
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
class UserCouponV1ApiE2ETest {

    private static final String ISSUE_ENDPOINT = "/api/v1/coupons";
    private static final String MY_COUPONS_ENDPOINT = "/api/v1/users/me/coupons";
    private static final String USERS_ENDPOINT = "/api/v1/users";
    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Test1234!";

    private final TestRestTemplate testRestTemplate;
    private final CouponJpaRepository couponJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserCouponV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        CouponJpaRepository couponJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.couponJpaRepository = couponJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
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
            USERS_ENDPOINT, HttpMethod.POST,
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
        return headers;
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue (쿠폰 발급)")
    @Nested
    class IssueCoupon {

        @DisplayName("유효한 쿠폰에 발급 요청 시, 201 Created와 발급 정보를 반환한다.")
        @Test
        void returnsCreated_whenCouponIsValid() {
            Coupon coupon = couponJpaRepository.save(
                new Coupon("10% 할인", CouponType.RATE, 10, 0, ZonedDateTime.now().plusYears(1))
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().get("userCouponId")).isNotNull(),
                () -> assertThat(response.getBody().data().get("status")).isEqualTo("AVAILABLE")
            );
        }

        @DisplayName("동일 쿠폰을 중복 발급 요청 시, 400 COUPON_ALREADY_ISSUED를 반환한다.")
        @Test
        void returnsBadRequest_whenDuplicateIssue() {
            Coupon coupon = couponJpaRepository.save(
                new Coupon("10% 할인", CouponType.RATE, 10, 0, ZonedDateTime.now().plusYears(1))
            );
            testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 발급 요청 시, 404를 반환한다.")
        @Test
        void returnsNotFound_whenCouponDoesNotExist() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/99999/issue",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("인증 실패 시, 401을 반환한다.")
        @Test
        void returnsUnauthorized_whenAuthFails() {
            Coupon coupon = couponJpaRepository.save(
                new Coupon("쿠폰", CouponType.FIXED, 1000, 0, ZonedDateTime.now().plusYears(1))
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons (내 쿠폰 목록 조회)")
    @Nested
    class GetMyCoupons {

        @DisplayName("발급받은 쿠폰이 있으면, 200 OK와 쿠폰 목록을 반환한다.")
        @Test
        void returnsMyCoupons_whenCouponsExist() {
            Coupon coupon = couponJpaRepository.save(
                new Coupon("10% 할인", CouponType.RATE, 10, 0, ZonedDateTime.now().plusYears(1))
            );
            testRestTemplate.exchange(
                ISSUE_ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    List<?> content = (List<?>) response.getBody().data().get("content");
                    assertThat(content).hasSize(1);
                }
            );
        }

        @DisplayName("발급받은 쿠폰이 없으면, 200 OK와 빈 목록을 반환한다.")
        @Test
        void returnsEmpty_whenNoCoupons() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    List<?> content = (List<?>) response.getBody().data().get("content");
                    assertThat(content).isEmpty();
                }
            );
        }

        @DisplayName("인증 실패 시, 401을 반환한다.")
        @Test
        void returnsUnauthorized_whenAuthFails() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", LOGIN_ID);
            headers.set("X-Loopers-LoginPw", "WrongPass1!");

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
