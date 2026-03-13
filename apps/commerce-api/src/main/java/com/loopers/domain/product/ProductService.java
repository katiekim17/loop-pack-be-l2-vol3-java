package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public List<Product> getProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllByIds(productIds);
        if (products.size() != productIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.");
        }
        return products;
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
    }

    @Transactional(readOnly = true)
    public List<Brand> getBrands(List<Long> brandIds) {
        List<Brand> brands = brandRepository.findAllByIds(brandIds);
        if (brands.size() != brandIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드가 포함되어 있습니다.");
        }
        return brands;
    }

    @Transactional(readOnly = true)
    public ProductDetail getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        Brand brand = brandRepository.findById(product.getBrandId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        List<ProductOption> options = productOptionRepository.findAllByProductId(productId);
        List<ProductImage> images = productImageRepository.findAllByProductId(productId);
        return new ProductDetail(product, brand, options, images);
    }

    @Transactional(readOnly = true)
    public Page<ProductListItem> getProductList(Long brandId, ProductSortType sortType, List<ProductStatus> statuses, Pageable pageable) {
        Page<Product> products = productRepository.findAll(brandId, sortType, statuses, pageable);
        List<Long> productIds = products.getContent().stream().map(Product::getId).toList();
        List<Long> brandIds = products.getContent().stream().map(Product::getBrandId).distinct().toList();

        Map<Long, Brand> brandMap = brandRepository.findAllByIds(brandIds).stream()
            .collect(Collectors.toMap(Brand::getId, b -> b));
        Map<Long, Long> minPriceMap = productOptionRepository.findAllByProductIds(productIds).stream()
            .collect(Collectors.groupingBy(
                ProductOption::getProductId,
                Collectors.collectingAndThen(
                    Collectors.minBy(java.util.Comparator.comparingLong(ProductOption::getPrice)),
                    opt -> opt.map(ProductOption::getPrice).orElse(0L)
                )
            ));

        return products.map(product -> new ProductListItem(
            product,
            brandMap.get(product.getBrandId()),
            minPriceMap.getOrDefault(product.getId(), 0L)
        ));
    }

    public record ProductDetail(Product product, Brand brand, List<ProductOption> options, List<ProductImage> images) {}

    public record ProductListItem(Product product, Brand brand, Long minPrice) {}
}