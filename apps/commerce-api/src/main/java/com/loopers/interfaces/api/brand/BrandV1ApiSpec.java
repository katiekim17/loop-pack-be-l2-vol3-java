package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandV1Dto.BrandResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface BrandV1ApiSpec {

  @GetMapping("/{brandId}")
  ApiResponse<BrandResponse> getBrands(
      @PathVariable(value = "brandId") Long brandId
  );
}
