package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductOption;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductImageJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.product.ProductOptionJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
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
class ProductsV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductOptionJpaRepository productOptionJpaRepository;
    private final ProductImageJpaRepository productImageJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ProductsV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        BrandJpaRepository brandJpaRepository,
        ProductJpaRepository productJpaRepository,
        ProductOptionJpaRepository productOptionJpaRepository,
        ProductImageJpaRepository productImageJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.productOptionJpaRepository = productOptionJpaRepository;
        this.productImageJpaRepository = productImageJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProduct {

        @DisplayName("유효한 productId를 주면, productId, name, description, brand, imageUrls, options, likeCount, createdAt을 반환한다.")
        @Test
        void returnsProductDetail_whenValidProductIdIsProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(
                new Brand("나이키", "스포츠 브랜드", "https://example.com/nike-logo.png")
            );
            Product product = productJpaRepository.save(
                new Product(brand.getId(), "에어맥스 90", new Money(150000L), "클래식 러닝화")
            );
            productOptionJpaRepository.save(
                new ProductOption(product.getId(), "265mm", new Money(150000L), 10L)
            );
            productImageJpaRepository.save(
                new ProductImage(product.getId(), "https://example.com/airmax-main.png")
            );

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(((Number) data.get("productId")).longValue()).isEqualTo(product.getId());
                    assertThat(data.get("name")).isEqualTo("에어맥스 90");
                    assertThat(data).containsKey("description");
                    assertThat(data).containsKey("brand");
                    assertThat(data).containsKey("imageUrls");
                    assertThat(data).containsKey("options");
                    assertThat(data).containsKey("likeCount");
                    assertThat(data).containsKey("createdAt");
                    Map<String, Object> brandData = (Map<String, Object>) data.get("brand");
                    assertThat(((Number) brandData.get("brandId")).longValue()).isEqualTo(brand.getId());
                    assertThat(brandData.get("name")).isEqualTo("나이키");
                    List<Map<String, Object>> imageUrls = (List<Map<String, Object>>) data.get("imageUrls");
                    assertThat(imageUrls).hasSize(1);
                    List<Map<String, Object>> options = (List<Map<String, Object>>) data.get("options");
                    assertThat(options).hasSize(1);
                }
            );
        }

        @DisplayName("존재하지 않는 productId를 주면, 404 Not Found를 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // arrange — DB에 아무 상품도 없음
            Long nonExistentId = 999999L;

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + nonExistentId,
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProductList {

        @DisplayName("조건 없이 조회하면, 전체 상품 목록을 페이지네이션 구조로 반환한다.")
        @Test
        void returnsPagedProductList_whenNoFilterProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", "스포츠 브랜드", "https://example.com/logo.png"));
            Product product1 = productJpaRepository.save(new Product(brand.getId(), "에어맥스 90", new Money(150000L), "러닝화"));
            Product product2 = productJpaRepository.save(new Product(brand.getId(), "조던 1", new Money(200000L), "농구화"));
            productOptionJpaRepository.save(new ProductOption(product1.getId(), "265mm", new Money(150000L), 5L));
            productOptionJpaRepository.save(new ProductOption(product2.getId(), "270mm", new Money(200000L), 3L));

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    assertThat(data).containsKey("content");
                    assertThat(data).containsKey("page");
                    assertThat(data).containsKey("size");
                    assertThat(data).containsKey("totalElements");
                    assertThat(data).containsKey("totalPages");
                    List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(2);
                }
            );
        }

        @DisplayName("brandId 필터를 주면, 해당 브랜드의 상품만 반환한다.")
        @Test
        void returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            Brand nike = brandJpaRepository.save(new Brand("나이키", "스포츠 브랜드", "https://example.com/nike.png"));
            Brand adidas = brandJpaRepository.save(new Brand("아디다스", "스포츠 브랜드", "https://example.com/adidas.png"));
            Product nikeProduct = productJpaRepository.save(new Product(nike.getId(), "에어맥스 90", new Money(150000L), "러닝화"));
            productJpaRepository.save(new Product(adidas.getId(), "울트라부스트", new Money(180000L), "러닝화"));
            productOptionJpaRepository.save(new ProductOption(nikeProduct.getId(), "265mm", new Money(150000L), 5L));

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?brandId=" + nike.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(1);
                    assertThat(content.get(0).get("name")).isEqualTo("에어맥스 90");
                }
            );
        }

        @DisplayName("sort=price_asc 파라미터를 주면, minPrice 오름차순으로 반환한다.")
        @Test
        void returnsProductsSortedByMinPriceAsc_whenSortParamProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product cheapProduct = productJpaRepository.save(new Product(brand.getId(), "저가 상품", new Money(50000L), null));
            Product expensiveProduct = productJpaRepository.save(new Product(brand.getId(), "고가 상품", new Money(200000L), null));
            productOptionJpaRepository.save(new ProductOption(cheapProduct.getId(), "S", new Money(50000L), 10L));
            productOptionJpaRepository.save(new ProductOption(expensiveProduct.getId(), "M", new Money(200000L), 10L));

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?sort=price_asc",
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
                    assertThat(content).hasSize(2);
                    long firstMinPrice = ((Number) content.get(0).get("minPrice")).longValue();
                    long secondMinPrice = ((Number) content.get(1).get("minPrice")).longValue();
                    assertThat(firstMinPrice).isLessThanOrEqualTo(secondMinPrice);
                }
            );
        }

        @DisplayName("상품이 없으면, 빈 content를 반환한다.")
        @Test
        void returnsEmptyContent_whenNoProductsExist() {
            // arrange — DB에 아무 상품도 없음

            // act
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(null),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> {
                    Map<String, Object> data = response.getBody().data();
                    List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
                    assertThat(content).isEmpty();
                }
            );
        }
    }
}