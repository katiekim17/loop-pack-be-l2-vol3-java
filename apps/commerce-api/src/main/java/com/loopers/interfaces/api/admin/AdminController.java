package com.loopers.interfaces.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1")
public class AdminController implements AdminV1ApiSpec {
  // (GET) /api-admin/v1/brands?page=0&size=20  // 등록된 브랜드 목록 조회
  // (GET) /api-admin/v1/brands/{brandId} // 브랜드 상세 조회
  // (POST) /api-admin/v1/brands  // 브랜드 등록
  // (PUT) /api-admin/v1/brands/{brandId}  // 브랜드 정보 수정
  // (DELETE) /api-admin/v1/brands/{brandId}  // 브랜드 삭제
  // (GET) /api-admin/v1/products?page=0&size=20&brandId={ brandId}  // 등록된 상품 목록 조회
  // (GET) /api-admin/v1/products/{productId}  // 상품 상세 조회
  // (POST) /api-admin/v1/products  // 상품 등록
  // (PUT) /api-admin/v1/products/{productId}  // 상품 정보 수정
  // (DELETE) /api-admin/v1/products/{productId}  // 상품 삭제
  // (POST) /api-admin/v1/orders  // 주문 요청
  // (GET) /api-admin/v1/orders?startAt=2026-01-31&endAt=2026-02-10  // 유저의 주문 목록 조회
  // (GET) /api-admin/v1/orders/{orderId}  // 단일 주문 상세 조회
}
