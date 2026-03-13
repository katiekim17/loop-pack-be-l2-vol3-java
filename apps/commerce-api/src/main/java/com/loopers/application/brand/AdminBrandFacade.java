package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandDeactivatedEvent;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class AdminBrandFacade {

    private final BrandRepository brandRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AdminBrandInfo createBrand(String name, String description, String logoImageUrl) {
        Brand brand = brandRepository.save(new Brand(name, description, logoImageUrl));
        return AdminBrandInfo.from(brand);
    }

    @Transactional
    public AdminBrandInfo updateBrand(Long brandId, String name, String description, String logoImageUrl) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        brand.updateInfo(name, description, logoImageUrl);
        return AdminBrandInfo.from(brandRepository.save(brand));
    }

    @Transactional
    public void deactivateBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        brand.deactivate();
        brandRepository.save(brand);
        // 브랜드 비활성화 이벤트 발행 → BrandEventListener가 비동기로 상품 연쇄 처리
        eventPublisher.publishEvent(new BrandDeactivatedEvent(brandId));
    }

    @Transactional(readOnly = true)
    public AdminBrandInfo getBrandInfo(Long brandId) {
        // 어드민은 INACTIVE 포함 모든 상태의 브랜드를 조회 가능
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다."));
        return AdminBrandInfo.from(brand);
    }

    @Transactional(readOnly = true)
    public Page<AdminBrandInfo> getBrandList(BrandStatus status, int page, int size) {
        // status가 null이면 전체, 아니면 해당 상태만 반환
        return brandRepository.findAll(status, PageRequest.of(page, size))
            .map(AdminBrandInfo::from);
    }
}
