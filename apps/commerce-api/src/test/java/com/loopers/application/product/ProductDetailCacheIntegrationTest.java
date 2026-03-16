package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductOption;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.product.ProductOptionJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SpringBootTest
class ProductDetailCacheIntegrationTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private AdminProductFacade adminProductFacade;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    private Product givenActiveProductWithOption(Brand brand) {
        Product product = productJpaRepository.save(
            new Product(brand.getId(), "에어맥스 90", new Money(150000L), "클래식 러닝화")
        );
        product.activate();
        productJpaRepository.save(product);
        productOptionJpaRepository.save(new ProductOption(product.getId(), "265mm", new Money(150000L), 10L));
        return product;
    }

    @DisplayName("캐시 저장")
    @Nested
    class CacheStore {

        @DisplayName("getProductDetail() 최초 호출 시 결과가 productDetail 캐시에 저장된다.")
        @Test
        void storesResultInCache_afterFirstCall() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = givenActiveProductWithOption(brand);

            // act
            productFacade.getProductDetail(product.getId());

            // assert
            Cache.ValueWrapper cached = cacheManager.getCache("productDetail").get(product.getId());
            assertThat(cached).isNotNull();
            assertThat(((ProductDetailInfo) cached.get()).productId()).isEqualTo(product.getId());
        }
    }

    @DisplayName("캐시 히트")
    @Nested
    class CacheHit {

        @DisplayName("캐시 적재 후 DB 데이터가 변경되어도 캐시된 값을 반환한다.")
        @Test
        void returnsCachedValue_evenWhenDbIsModified() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = givenActiveProductWithOption(brand);

            // 첫 번째 호출 - "에어맥스 90" 캐시 적재
            productFacade.getProductDetail(product.getId());

            // DB를 직접 변경 (캐시 우회)
            product.updateInfo(brand.getId(), "에어맥스 90 리뉴얼", new Money(160000L), "신상품", null);
            productJpaRepository.save(product);

            // act - 두 번째 호출
            ProductDetailInfo result = productFacade.getProductDetail(product.getId());

            // assert - DB 변경값이 아닌 캐시된 값 반환
            assertThat(result.name()).isEqualTo("에어맥스 90");
        }
    }

    @DisplayName("캐시 무효화")
    @Nested
    class CacheEvict {

        @DisplayName("updateProduct() 호출 시 해당 productId의 캐시가 제거된다.")
        @Test
        void evictsProductDetailCache_whenProductIsUpdated() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = givenActiveProductWithOption(brand);

            productFacade.getProductDetail(product.getId());
            assertThat(cacheManager.getCache("productDetail").get(product.getId())).isNotNull();

            // act
            adminProductFacade.updateProduct(
                product.getId(), brand.getId(), "에어맥스 90 리뉴얼", 160000L, "신상품", null
            );

            // assert
            assertThat(cacheManager.getCache("productDetail").get(product.getId())).isNull();
        }

        @DisplayName("deactivateProduct() 호출 시 해당 productId의 캐시가 제거된다.")
        @Test
        void evictsProductDetailCache_whenProductIsDeactivated() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product = givenActiveProductWithOption(brand);

            productFacade.getProductDetail(product.getId());
            assertThat(cacheManager.getCache("productDetail").get(product.getId())).isNotNull();

            // act
            adminProductFacade.deactivateProduct(product.getId());

            // assert
            assertThat(cacheManager.getCache("productDetail").get(product.getId())).isNull();
        }

        @DisplayName("다른 상품의 updateProduct() 호출 시 해당 상품의 캐시는 유지된다.")
        @Test
        void doesNotEvictOtherProductCache_whenDifferentProductIsUpdated() {
            // arrange
            Brand brand = brandJpaRepository.save(new Brand("나이키", null, null));
            Product product1 = givenActiveProductWithOption(brand);
            Product product2 = givenActiveProductWithOption(brand);

            productFacade.getProductDetail(product1.getId());
            productFacade.getProductDetail(product2.getId());

            // act - product2만 수정
            adminProductFacade.updateProduct(
                product2.getId(), brand.getId(), "수정된 상품", 160000L, null, null
            );

            // assert - product1 캐시는 유지
            assertThat(cacheManager.getCache("productDetail").get(product1.getId())).isNotNull();
            // product2 캐시는 제거
            assertThat(cacheManager.getCache("productDetail").get(product2.getId())).isNull();
        }
    }
}
