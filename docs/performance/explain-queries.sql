-- ============================================================
-- 상품 목록 조회 EXPLAIN ANALYZE 쿼리 모음
-- 더미 데이터 10만 건 기준, 인덱스 적용 전/후 동일 쿼리 실행
-- docker cp docs/performance/explain-queries.sql docker-mysql-1:/tmp/explain.sql
-- ============================================================

-- ============================================================
-- UC-1 좋아요순 정렬 + 전체 조회
-- http://localhost:8080/api/v1/products?page=0&size=20
-- ============================================================
-- 상품 목록 조회용
EXPLAIN ANALYZE
select
    p1_0.id,
    p1_0.brand_id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.description,
    p1_0.like_count,
    p1_0.name,
    p1_0.price,
    p1_0.status,
    p1_0.thumbnail_image_url,
    p1_0.updated_at
from product p1_0
where p1_0.status in ('ACTIVE', 'OUT_OF_STOCK')
order by p1_0.created_at desc
    limit 0, 20;
-- count 쿼리용
EXPLAIN ANALYZE
select count(p1_0.id)
from product p1_0
where p1_0.status in ('ACTIVE', 'OUT_OF_STOCK');
-- 브랜드 조회용
EXPLAIN ANALYZE
select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (
                  20,18,15,9,6,5,4,2,1,19,13,10,8,3
    );
-- 상품 옵션 조회용
EXPLAIN ANALYZE
select
    po1_0.id,
    po1_0.created_at,
    po1_0.deleted_at,
    po1_0.name,
    po1_0.price,
    po1_0.product_id,
    po1_0.stock_quantity,
    po1_0.updated_at
from product_option po1_0
where po1_0.product_id in (
                           96361,87601,78841,61321,52561,
                           43801,35041,17521,8761,1,
                           91251,73731,64971,56211,47451,
                           29931,21171,12411,3651,94901
    );
-- ============================================================
-- UC-2 좋아요순 정렬 + 전체 조회
-- http://localhost:8080/api/v1/products?sort=likes_desc&page=0&size=20
-- ============================================================
-- 상품 목록 조회용
EXPLAIN ANALYZE
select
    p1_0.id,
    p1_0.brand_id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.description,
    p1_0.like_count,
    p1_0.name,
    p1_0.price,
    p1_0.status,
    p1_0.thumbnail_image_url,
    p1_0.updated_at
from product p1_0
where p1_0.status in ('ACTIVE', 'OUT_OF_STOCK')
order by p1_0.like_count desc
    limit 0, 20;

-- count 쿼리용
EXPLAIN ANALYZE
select count(p1_0.id)
from product p1_0
where p1_0.status in ('ACTIVE', 'OUT_OF_STOCK');

-- 브랜드 조회용
EXPLAIN ANALYZE
select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (15, 5, 1, 4, 20, 12, 2, 3, 9, 19);

-- 상품 옵션 조회용
EXPLAIN ANALYZE
select
    po1_0.id,
    po1_0.created_at,
    po1_0.deleted_at,
    po1_0.name,
    po1_0.price,
    po1_0.product_id,
    po1_0.stock_quantity,
    po1_0.updated_at
from product_option po1_0
where po1_0.product_id in (
   80000, 50000, 10000, 40000, 100000,
   70000, 20000, 30000, 60000, 90000,
   79979, 9979, 99979, 69979, 49979,
   39979, 29979, 89979, 19979, 59979
);

-- ============================================================
-- UC-3 브랜드 필터 + 최신순
-- http://localhost:8080/api/v1/products?brandId=1&page=0&size=20
-- ============================================================
-- 상품 목록 조회용
EXPLAIN ANALYZE select
    p1_0.id,
    p1_0.brand_id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.description,
    p1_0.like_count,
    p1_0.name,
    p1_0.price,
    p1_0.status,
    p1_0.thumbnail_image_url,
    p1_0.updated_at
from product p1_0
where p1_0.brand_id = 1
  and p1_0.status in ('ACTIVE', 'OUT_OF_STOCK')
order by p1_0.created_at desc
    limit 0, 20;
-- count 쿼리용
EXPLAIN ANALYZE select count(p1_0.id)
from product p1_0
where p1_0.brand_id = 1
  and p1_0.status in ('ACTIVE', 'OUT_OF_STOCK');
-- 브랜드 조회용
EXPLAIN ANALYZE select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (1);
-- 상품 옵션 조회용
EXPLAIN ANALYZE select
    po1_0.id,
    po1_0.created_at,
    po1_0.deleted_at,
    po1_0.name,
    po1_0.price,
    po1_0.product_id,
    po1_0.stock_quantity,
    po1_0.updated_at
from product_option po1_0
where po1_0.product_id in (
                           8761,1,3651,7301,5841,
                           731,8031,2921,6571,1461,
                           5111,8762,2,3652,7302,
                           5842,732,8032,2922,6572
    );
-- ============================================================
-- UC-4 브랜드 필터 + 좋아요순
-- http://localhost:8080/api/v1/products?brandId=1&sort=likes_desc&page=0&size=20
-- ============================================================
-- 상품 목록 조회용
EXPLAIN ANALYZE
select
    p1_0.id,
    p1_0.brand_id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.description,
    p1_0.like_count,
    p1_0.name,
    p1_0.price,
    p1_0.status,
    p1_0.thumbnail_image_url,
    p1_0.updated_at
from product p1_0
where p1_0.brand_id = 1
  and p1_0.status in ('ACTIVE','OUT_OF_STOCK')
order by p1_0.like_count desc
    limit 0, 20;
-- count 쿼리용
EXPLAIN ANALYZE
select count(p1_0.id)
from product p1_0
where p1_0.brand_id = 1
  and p1_0.status in ('ACTIVE','OUT_OF_STOCK');
-- 브랜드 조회용
EXPLAIN ANALYZE
select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (1);
-- 상품 옵션 조회용
EXPLAIN ANALYZE
select
    po1_0.id,
    po1_0.created_at,
    po1_0.deleted_at,
    po1_0.name,
    po1_0.price,
    po1_0.product_id,
    po1_0.stock_quantity,
    po1_0.updated_at
from product_option po1_0
where po1_0.product_id in (
                           10000,9979,9978,9977,9976,
                           9975,9974,9973,9972,9971,
                           9970,9969,9968,9967,9966,
                           9965,9964,9963,9962,9961
    );
-- ============================================================
-- UC-5 브랜드 필터 + 가격순
-- http://localhost:8080/api/v1/products?brandId=1&sort=price_asc&page=0&size=20
-- ============================================================
-- 상품 목록 조회용
EXPLAIN ANALYZE
select
    p1_0.id,
    p1_0.brand_id,
    p1_0.created_at,
    p1_0.deleted_at,
    p1_0.description,
    p1_0.like_count,
    p1_0.name,
    p1_0.price,
    p1_0.status,
    p1_0.thumbnail_image_url,
    p1_0.updated_at
from product p1_0
where p1_0.brand_id = 1
  and p1_0.status in ('ACTIVE', 'OUT_OF_STOCK')
order by p1_0.price
    limit 0, 20;
-- count 쿼리용
EXPLAIN ANALYZE
select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (1);
-- 브랜드 조회용
EXPLAIN ANALYZE
select
    b1_0.id,
    b1_0.created_at,
    b1_0.deleted_at,
    b1_0.description,
    b1_0.logo_image_url,
    b1_0.name,
    b1_0.status,
    b1_0.updated_at
from brand b1_0
where b1_0.id in (1);
-- 상품 옵션 조회용
EXPLAIN ANALYZE
select
    po1_0.id,
    po1_0.created_at,
    po1_0.deleted_at,
    po1_0.name,
    po1_0.price,
    po1_0.product_id,
    po1_0.stock_quantity,
    po1_0.updated_at
from product_option po1_0
where po1_0.product_id in (
                           2001,1001,8001,3001,1,
                           7001,4001,6001,5001,9001,
                           3002,2002,8002,4002,7002,
                           6002,9002,5002,1002,2
    );