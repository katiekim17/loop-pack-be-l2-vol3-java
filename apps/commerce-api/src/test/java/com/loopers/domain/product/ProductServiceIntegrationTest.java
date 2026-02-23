package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품을 조회할 때, ")
    @Nested
    class GetProducts {

        @DisplayName("존재하는 상품 ID 목록을 주면, 해당 상품 목록을 반환한다.")
        @Test
        void returnsProducts_whenValidIdsAreProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));
            Product product1 = productJpaRepository.save(new Product(brand.getId(), "신발A", new Money(50000L), "설명A"));
            Product product2 = productJpaRepository.save(new Product(brand.getId(), "신발B", new Money(60000L), "설명B"));

            // act
            List<Product> result = productService.getProducts(List.of(product1.getId(), product2.getId()));

            // assert
            assertThat(result).hasSize(2);
        }

        @DisplayName("존재하지 않는 상품 ID가 포함되면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenAnyIdDoesNotExist() {
            // arrange
            Long nonExistentId = 999L;

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                productService.getProducts(List.of(nonExistentId))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("브랜드를 조회할 때, ")
    @Nested
    class GetBrands {

        @DisplayName("존재하는 브랜드 ID 목록을 주면, 해당 브랜드 목록을 반환한다.")
        @Test
        void returnsBrands_whenValidIdsAreProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키"));

            // act
            List<com.loopers.domain.brand.Brand> result = productService.getBrands(List.of(brand.getId()));

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("나이키");
        }

        @DisplayName("존재하지 않는 브랜드 ID가 포함되면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenAnyBrandIdDoesNotExist() {
            // arrange
            Long nonExistentId = 999L;

            // act
            CoreException result = assertThrows(CoreException.class, () ->
                productService.getBrands(List.of(nonExistentId))
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
