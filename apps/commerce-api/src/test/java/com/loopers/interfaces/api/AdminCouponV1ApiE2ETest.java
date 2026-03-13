package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.ZonedDateTime;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponV1ApiE2ETest {

    private static final String COUPON_ENDPOINT = "/api-admin/v1/coupons";
    private static final String LDAP_HEADER = "X-Loopers-Ldap";
    private static final String ADMIN_LDAP = "loopers.admin";

    private final TestRestTemplate testRestTemplate;
    private final CouponJpaRepository couponJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public AdminCouponV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        CouponJpaRepository couponJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.couponJpaRepository = couponJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(LDAP_HEADER, ADMIN_LDAP);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> validCreateRequest() {
        return Map.of(
            "name", "신규 10% 할인",
            "type", "RATE",
            "value", 10,
            "minOrderAmount", 10000,
            "expiredAt", ZonedDateTime.now().plusYears(1).toString()
        );
    }

    @DisplayName("POST /api-admin/v1/coupons (쿠폰 템플릿 등록)")
    @Nested
    class CreateCoupon {

        @DisplayName("LDAP 헤더 없이 요청하면 403을 반환한다.")
        @Test
        void returnsForbidden_whenLdapHeaderIsMissing() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @DisplayName("유효한 요청이면 201 Created와 쿠폰 정보를 반환한다.")
        @Test
        void returnsCreated_whenValidRequest() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("couponId")).isNotNull();
                    assertThat(data.get("name")).isEqualTo("신규 10% 할인");
                    assertThat(data.get("type")).isEqualTo("RATE");
                }
            );
        }

        @DisplayName("name이 없으면 400을 반환한다.")
        @Test
        void returnsBadRequest_whenNameIsMissing() {
            Map<String, Object> request = Map.of(
                "type", "RATE",
                "value", 10,
                "expiredAt", ZonedDateTime.now().plusYears(1).toString()
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("RATE 타입에서 value가 100 초과이면 400을 반환한다.")
        @Test
        void returnsBadRequest_whenRateValueExceeds100() {
            Map<String, Object> request = Map.of(
                "name", "할인", "type", "RATE", "value", 101,
                "expiredAt", ZonedDateTime.now().plusYears(1).toString()
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(request, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{id} (쿠폰 템플릿 수정)")
    @Nested
    class UpdateCoupon {

        @DisplayName("유효한 요청이면, 200 OK와 수정된 쿠폰 정보를 반환한다.")
        @Test
        void returnsOk_whenValidRequest() {
            // create first
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long couponId = ((Number) createResponse.getBody().data().get("couponId")).longValue();

            Map<String, Object> updateRequest = Map.of(
                "name", "수정된 이름",
                "minOrderAmount", 20000,
                "expiredAt", ZonedDateTime.now().plusMonths(6).toString()
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/" + couponId, HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("수정된 이름")
            );
        }

        @DisplayName("type 변경 시도 시, 400 COUPON_TYPE_IMMUTABLE을 반환한다.")
        @Test
        void returnsBadRequest_whenTypeChangeIsAttempted() {
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long couponId = ((Number) createResponse.getBody().data().get("couponId")).longValue();

            Map<String, Object> updateRequest = Map.of(
                "name", "이름",
                "type", "FIXED",
                "expiredAt", ZonedDateTime.now().plusYears(1).toString()
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/" + couponId, HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("존재하지 않는 쿠폰 수정 시, 404를 반환한다.")
        @Test
        void returnsNotFound_whenCouponDoesNotExist() {
            Map<String, Object> updateRequest = Map.of(
                "name", "이름",
                "expiredAt", ZonedDateTime.now().plusYears(1).toString()
            );

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/99999", HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{id} (쿠폰 템플릿 삭제)")
    @Nested
    class DeleteCoupon {

        @DisplayName("정상 삭제 시, 204 No Content를 반환한다.")
        @Test
        void returnsNoContent_whenDeleted() {
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long couponId = ((Number) createResponse.getBody().data().get("couponId")).longValue();

            ResponseEntity<Void> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/" + couponId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()),
                Void.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @DisplayName("이미 삭제된 쿠폰 재삭제 시, 404를 반환한다.")
        @Test
        void returnsNotFound_whenAlreadyDeleted() {
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long couponId = ((Number) createResponse.getBody().data().get("couponId")).longValue();

            testRestTemplate.exchange(COUPON_ENDPOINT + "/" + couponId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()), Void.class);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/" + couponId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api-admin/v1/coupons (쿠폰 목록 조회)")
    @Nested
    class GetCoupons {

        @DisplayName("200 OK와 삭제되지 않은 쿠폰 목록을 반환한다.")
        @Test
        void returnsActiveCoupons() {
            testRestTemplate.exchange(COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()), new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("content")).isNotNull()
            );
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{id} (쿠폰 상세 조회)")
    @Nested
    class GetCoupon {

        @DisplayName("존재하는 쿠폰 조회 시, 200 OK와 쿠폰 정보를 반환한다.")
        @Test
        void returnsOk_whenCouponExists() {
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(validCreateRequest(), adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long couponId = ((Number) createResponse.getBody().data().get("couponId")).longValue();

            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/" + couponId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("couponId")).isNotNull()
            );
        }

        @DisplayName("존재하지 않는 쿠폰 조회 시, 404를 반환한다.")
        @Test
        void returnsNotFound_whenCouponDoesNotExist() {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                COUPON_ENDPOINT + "/99999", HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
