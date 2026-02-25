package com.loopers.application.product;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductHistory;
import com.loopers.domain.product.ProductHistoryRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class AdminProductFacade {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductHistoryRepository productHistoryRepository;

    @Transactional
    public AdminProductInfo createProduct(Long brandId, String name, Long price, String description, String thumbnailImageUrl) {
        brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        Product product = productRepository.save(
            new Product(brandId, name, new Money(price), description, thumbnailImageUrl)
        );
        // 등록 시 버전 1의 스냅샷 자동 저장
        int version = productHistoryRepository.countByProductId(product.getId()) + 1;
        productHistoryRepository.save(ProductHistory.snapshot(product, version, "admin"));
        return AdminProductInfo.from(product);
    }

    @Transactional
    public AdminProductInfo updateProduct(Long productId, Long brandId, String name, Long price, String description, String thumbnailImageUrl) {
        brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        product.updateInfo(brandId, name, new Money(price), description, thumbnailImageUrl);
        productRepository.save(product);
        // 수정 시 버전이 1 증가한 스냅샷 저장
        int version = productHistoryRepository.countByProductId(productId) + 1;
        productHistoryRepository.save(ProductHistory.snapshot(product, version, "admin"));
        return AdminProductInfo.from(product);
    }

    @Transactional
    public void deactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        product.deactivate();
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public AdminProductInfo getProductInfo(Long productId) {
        // 어드민은 PENDING/INACTIVE 포함 모든 상태의 상품 조회 가능
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        return AdminProductInfo.from(product);
    }

    @Transactional(readOnly = true)
    public Page<AdminProductInfo> getProductList(Long brandId, ProductStatus status, int page, int size) {
        // status가 null이면 전체 조회, 아니면 해당 상태만 반환
        if (status != null) {
            return productRepository.findAll(brandId, ProductSortType.LATEST, List.of(status), PageRequest.of(page, size))
                .map(AdminProductInfo::from);
        }
        return productRepository.findAll(brandId, ProductSortType.LATEST, PageRequest.of(page, size))
            .map(AdminProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductHistoryInfo> getProductHistory(Long productId, int page, int size) {
        return productHistoryRepository.findAllByProductId(productId, PageRequest.of(page, size))
            .map(ProductHistoryInfo::from);
    }
}
