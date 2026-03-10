-- ============================================================
-- 상품 목록 조회 인덱스 DDL
-- 적용 전 EXPLAIN 결과 확인 후 실행
-- ============================================================

-- UC-1: 전체 조회 + 최신순
CREATE INDEX idx_product_status_created
    ON product (status, created_at DESC);

-- UC-2: 전체 조회 + 좋아요순
CREATE INDEX idx_product_status_like
    ON product (status, like_count DESC);

-- UC-3: 브랜드 필터 + 최신순
CREATE INDEX idx_product_brand_status_created
    ON product (brand_id, status, created_at DESC);

-- UC-4: 브랜드 필터 + 좋아요순
CREATE INDEX idx_product_brand_status_like
    ON product (brand_id, status, like_count DESC);

-- UC-5: 브랜드 필터 + 가격 오름차순
CREATE INDEX idx_product_brand_status_price
    ON product (brand_id, status, price ASC);

-- ============================================================
-- 롤백 (인덱스 제거)
-- ============================================================
-- DROP INDEX idx_product_status_created        ON product;
-- DROP INDEX idx_product_status_like           ON product;
-- DROP INDEX idx_product_brand_status_created  ON product;
-- DROP INDEX idx_product_brand_status_like     ON product;
-- DROP INDEX idx_product_brand_status_price    ON product;
