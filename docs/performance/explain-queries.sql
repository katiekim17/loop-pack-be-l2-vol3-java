-- ============================================================
-- 상품 목록 조회 EXPLAIN ANALYZE 쿼리 모음
-- 더미 데이터 10만 건 기준, 인덱스 적용 전/후 동일 쿼리 실행
-- ============================================================

-- UC-1: 전체 조회 + 최신순 (기본)
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- UC-2: 전체 조회 + 좋아요순
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY like_count DESC
LIMIT 20 OFFSET 0;

-- UC-3: 브랜드 필터 + 최신순
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE brand_id = 1
  AND status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- UC-4: 브랜드 필터 + 좋아요순
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE brand_id = 1
  AND status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY like_count DESC
LIMIT 20 OFFSET 0;

-- UC-5: 브랜드 필터 + 가격 오름차순
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE brand_id = 1
  AND status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY price ASC
LIMIT 20 OFFSET 0;

-- ============================================================
-- 참고: 실제 애플리케이션 실행 쿼리 (JPA 생성 형태)
-- (:brandId IS NULL OR brand_id = :brandId) 패턴 포함
-- ============================================================

-- UC-3 JPA 실제 쿼리 형태
EXPLAIN ANALYZE
SELECT *
FROM product
WHERE (1 IS NULL OR brand_id = 1)
  AND status IN ('ACTIVE', 'OUT_OF_STOCK')
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
