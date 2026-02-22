package com.loopers.interfaces.api.product;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/Products")
public class ProductsController implements ProductsV1ApiSpec {
  // /api/v1/products  // 상품 목록 조회
  // /api/v1/products/{productId}  // 상품 정보 조회
  // (POST) /api/v1/products/{productId}/likes   // 상품 좋아요 등록
  // (DELETE) /api/v1/products/{productId}/likes   // 상품 좋아요 취소

}
