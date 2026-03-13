package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductHistoryJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
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
class AdminV1ApiE2ETest {

    private static final String BRAND_ENDPOINT = "/api-admin/v1/brands";
    private static final String PRODUCT_ENDPOINT = "/api-admin/v1/products";
    private static final String LDAP_HEADER = "X-Loopers-Ldap";
    private static final String ADMIN_LDAP = "loopers.admin";

    private final TestRestTemplate testRestTemplate;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductHistoryJpaRepository productHistoryJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public AdminV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        BrandJpaRepository brandJpaRepository,
        ProductJpaRepository productJpaRepository,
        ProductHistoryJpaRepository productHistoryJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.productHistoryJpaRepository = productHistoryJpaRepository;
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

    // ─────────────────────────────────────────────
    // 브랜드 관리
    // ─────────────────────────────────────────────

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    class CreateBrand {

        @DisplayName("LDAP 헤더 없이 요청하면 403을 반환한다.")
        @Test
        void returnsForbidden_whenLdapHeaderIsMissing() {
            // arrange
            String body = """
                {"name": "나이키", "description": "스포츠 브랜드", "logoImageUrl": "https://example.com/logo.png"}
                """;

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(body, new HttpHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @DisplayName("유효한 요청으로 브랜드를 등록하면 201과 브랜드 정보를 반환한다.")
        @Test
        void returnsBrandInfo_whenValidRequestProvided() {
            // arrange
            String body = """
                {"name": "나이키", "description": "스포츠 브랜드", "logoImageUrl": "https://example.com/logo.png"}
                """;

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("name")).isEqualTo("나이키");
                    assertThat(data.get("status")).isEqualTo("PENDING");
                    assertThat(data).containsKey("brandId");
                    assertThat(data).containsKey("createdAt");
                }
            );
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    class UpdateBrand {

        @DisplayName("브랜드 정보를 수정하면 200과 수정된 정보를 반환한다.")
        @Test
        void returnsUpdatedBrand_whenValidRequestProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", "스포츠", "https://example.com/logo.png"));
            String body = """
                {"name": "나이키 코리아", "description": "한국 나이키", "logoImageUrl": "https://example.com/new-logo.png"}
                """;

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT + "/" + brand.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(body, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("나이키 코리아")
            );
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    class DeactivateBrand {

        @DisplayName("브랜드를 비활성화하면 204를 반환하고, 해당 브랜드의 상품도 INACTIVE가 된다.")
        @Test
        void returnsNoContent_andDeactivatesProducts_whenBrandDeactivated() throws InterruptedException {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            brand.activate();
            brandJpaRepository.save(brand);

            Product product = productJpaRepository.save(
                new Product(brand.getId(), "에어맥스", new Money(150000L), null)
            );
            product.activate();
            productJpaRepository.save(product);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                BRAND_ENDPOINT + "/" + brand.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()),
                Void.class
            );

            // assert - 브랜드 비활성화 응답
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // assert - 상품 비동기 비활성화 (최대 3초 대기)
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline) {
                Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
                if ("INACTIVE".equals(updated.getStatus().name())) {
                    break;
                }
                Thread.sleep(100);
            }
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStatus().name()).isEqualTo("INACTIVE");
        }
    }

    // ─────────────────────────────────────────────
    // 상품 관리
    // ─────────────────────────────────────────────

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    class CreateProduct {

        @DisplayName("존재하지 않는 브랜드로 상품 등록 시 404를 반환한다.")
        @Test
        void returnsNotFound_whenBrandDoesNotExist() {
            // arrange
            String body = """
                {"brandId": 999999, "name": "에어맥스", "price": 150000, "description": "러닝화"}
                """;

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("유효한 요청으로 상품을 등록하면 201과 상품 정보를 반환하고 이력이 1건 저장된다.")
        @Test
        void returnsProductInfo_andSavesHistory_whenValidRequestProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            String body = String.format("""
                {"brandId": %d, "name": "에어맥스", "price": 150000, "description": "러닝화"}
                """, brand.getId());

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data.get("name")).isEqualTo("에어맥스");
                    assertThat(data.get("status")).isEqualTo("PENDING");
                    assertThat(data).containsKey("productId");
                },
                () -> {
                    Long productId = ((Number) response.getBody().data().get("productId")).longValue();
                    assertThat(productHistoryJpaRepository.countByProductId(productId)).isEqualTo(1);
                }
            );
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    class UpdateProduct {

        @DisplayName("상품 수정 시 200을 반환하고 이력이 2건이 된다.")
        @Test
        void returnsUpdatedProduct_andHistoryCount2_whenProductUpdated() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            // 상품 등록 (이력 1건 생성)
            String createBody = String.format("""
                {"brandId": %d, "name": "에어맥스", "price": 150000, "description": "러닝화"}
                """, brand.getId());
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(createBody, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long productId = ((Number) createResponse.getBody().data().get("productId")).longValue();

            // 상품 수정
            String updateBody = String.format("""
                {"brandId": %d, "name": "에어맥스 90", "price": 160000, "description": "클래식 러닝화"}
                """, brand.getId());

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "/" + productId,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("에어맥스 90"),
                () -> assertThat(productHistoryJpaRepository.countByProductId(productId)).isEqualTo(2)
            );
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}/history")
    @Nested
    class GetProductHistory {

        @DisplayName("상품 이력을 조회하면 200과 이력 목록을 반환한다.")
        @Test
        void returnsHistoryList_whenProductHistoryRequested() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            String createBody = String.format("""
                {"brandId": %d, "name": "에어맥스", "price": 150000}
                """, brand.getId());
            ResponseEntity<ApiResponse<Map<String, Object>>> createResponse = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(createBody, adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );
            Long productId = ((Number) createResponse.getBody().data().get("productId")).longValue();

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "/" + productId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data).containsKey("content");
                    assertThat(data).containsKey("totalElements");
                }
            );
        }
    }

    // ─────────────────────────────────────────────
    // 브랜드 조회
    // ─────────────────────────────────────────────

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    class GetAdminBrand {

        @DisplayName("존재하는 brandId를 조회하면 200과 브랜드 상세 정보를 반환한다.")
        @Test
        void returnsBrandDetail_whenBrandExists() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", "스포츠 브랜드", "https://example.com/logo.png"));

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT + "/" + brand.getId(),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(((Number) data.get("brandId")).longValue()).isEqualTo(brand.getId());
                    assertThat(data.get("name")).isEqualTo("나이키");
                    assertThat(data.get("status")).isEqualTo("PENDING");
                    assertThat(data).containsKey("createdAt");
                    assertThat(data).containsKey("updatedAt");
                }
            );
        }

        @DisplayName("존재하지 않는 brandId를 조회하면 404를 반환한다.")
        @Test
        void returnsNotFound_whenBrandDoesNotExist() {
            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT + "/999999",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    class GetAdminBrandList {

        @DisplayName("status=ACTIVE 필터를 주면 ACTIVE 브랜드만 반환한다.")
        @Test
        void returnsActiveBrandsOnly_whenStatusFilterProvided() {
            // arrange
            brandJpaRepository.save(new Brand("나이키", null, null)); // PENDING

            Brand active = brandJpaRepository.save(new Brand("아디다스", null, null));
            active.activate();
            brandJpaRepository.save(active);

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT + "?status=ACTIVE",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    java.util.List<Map<String, Object>> content =
                        (java.util.List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(1);
                    assertThat(content.get(0).get("name")).isEqualTo("아디다스");
                }
            );
        }

        @DisplayName("필터 없이 조회하면 모든 상태의 브랜드 목록을 반환한다.")
        @Test
        void returnsBrandList_whenNoFilterProvided() {
            // arrange
            brandJpaRepository.save(new Brand("나이키", null, null)); // PENDING
            Brand active = brandJpaRepository.save(new Brand("아디다스", null, null));
            active.activate();
            brandJpaRepository.save(active);

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    java.util.List<Map<String, Object>> content =
                        (java.util.List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(2);
                }
            );
        }
    }

    // ─────────────────────────────────────────────
    // 상품 조회 / 비활성화
    // ─────────────────────────────────────────────

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    class GetAdminProduct {

        @DisplayName("존재하는 productId를 조회하면 200과 상품 상세 정보를 반환한다.")
        @Test
        void returnsProductDetail_whenProductExists() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = productJpaRepository.save(
                new Product(brand.getId(), "에어맥스", new Money(150000L), "러닝화")
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(((Number) data.get("productId")).longValue()).isEqualTo(product.getId());
                    assertThat(data.get("name")).isEqualTo("에어맥스");
                    assertThat(data.get("status")).isEqualTo("PENDING");
                    assertThat(data.get("price")).isEqualTo(150000);
                    assertThat(data).containsKey("brandId");
                    assertThat(data).containsKey("createdAt");
                }
            );
        }

        @DisplayName("존재하지 않는 productId를 조회하면 404를 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "/999999",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api-admin/v1/products")
    @Nested
    class GetAdminProductList {

        @DisplayName("brandId 필터를 주면 해당 브랜드의 상품만 반환한다.")
        @Test
        void returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            Brand nike = brandJpaRepository.save(new Brand("나이키", null, null));
            Brand adidas = brandJpaRepository.save(new Brand("아디다스", null, null));
            productJpaRepository.save(new Product(nike.getId(), "에어맥스", new Money(150000L), null));
            productJpaRepository.save(new Product(adidas.getId(), "울트라부스트", new Money(180000L), null));

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "?brandId=" + nike.getId(),
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    java.util.List<Map<String, Object>> content =
                        (java.util.List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(1);
                    assertThat(content.get(0).get("name")).isEqualTo("에어맥스");
                }
            );
        }

        @DisplayName("status=ACTIVE 필터를 주면 ACTIVE 상품만 반환한다.")
        @Test
        void returnsActiveProductsOnly_whenStatusFilterProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            productJpaRepository.save(new Product(brand.getId(), "PENDING상품", new Money(100000L), null)); // PENDING

            Product activeProduct = productJpaRepository.save(
                new Product(brand.getId(), "ACTIVE상품", new Money(150000L), null)
            );
            activeProduct.activate();
            productJpaRepository.save(activeProduct);

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "?status=ACTIVE",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    java.util.List<Map<String, Object>> content =
                        (java.util.List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(1);
                    assertThat(content.get(0).get("name")).isEqualTo("ACTIVE상품");
                }
            );
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    class DeactivateProduct {

        @DisplayName("상품을 비활성화하면 204를 반환하고 상태가 INACTIVE가 된다.")
        @Test
        void returnsNoContent_andDeactivatesProduct_whenProductDeactivated() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = productJpaRepository.save(
                new Product(brand.getId(), "에어맥스", new Money(150000L), null)
            );
            product.activate();
            productJpaRepository.save(product);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT + "/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders()),
                Void.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStatus().name()).isEqualTo("INACTIVE");
        }
    }

    // ─────────────────────────────────────────────
    // 고객 API 상태 필터링
    // ─────────────────────────────────────────────

    @DisplayName("고객 API - INACTIVE 브랜드 조회 시 404를 반환한다.")
    @Test
    void returnsNotFound_whenBrandIsInactive() {
        // arrange
        Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
        brand.deactivate();
        brandJpaRepository.save(brand);

        // act
        ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
            "/api/v1/brands/" + brand.getId(),
            HttpMethod.GET,
            new HttpEntity<>(null),
            new ParameterizedTypeReference<>() {}
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("고객 API - INACTIVE 상품은 상품 목록에 포함되지 않는다.")
    @Test
    void excludesInactiveProducts_fromCustomerProductList() {
        // arrange
        Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
        brand.activate();
        brandJpaRepository.save(brand);

        Product activeProduct = productJpaRepository.save(
            new Product(brand.getId(), "에어맥스", new Money(150000L), null)
        );
        activeProduct.activate();
        productJpaRepository.save(activeProduct);

        Product inactiveProduct = productJpaRepository.save(
            new Product(brand.getId(), "단종 상품", new Money(50000L), null)
        );
        inactiveProduct.deactivate();
        productJpaRepository.save(inactiveProduct);

        // act
        ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
            "/api/v1/products",
            HttpMethod.GET,
            new HttpEntity<>(null),
            new ParameterizedTypeReference<>() {}
        );

        // assert
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> {
                Map<String, Object> data = response.getBody().data();
                java.util.List<Map<String, Object>> content =
                    (java.util.List<Map<String, Object>>) data.get("content");
                assertThat(content).hasSize(1);
                assertThat(content.get(0).get("name")).isEqualTo("에어맥스");
            }
        );
    }
}
