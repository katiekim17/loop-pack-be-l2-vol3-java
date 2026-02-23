package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.brand.BrandService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandV1Dto.BrandResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/brands")
public class BrandController implements BrandV1ApiSpec {

  private final BrandService brandService;

  @GetMapping("/{brandId}")
  @Override
  public ApiResponse<BrandResponse> getBrands(
      @PathVariable(value = "brandId") Long brandId
  ) {
    BrandInfo info = brandService.getBrandInfo(brandId);
    BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(info);
    return ApiResponse.success(response);
  }

}
