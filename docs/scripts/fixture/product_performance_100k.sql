-- 삽입 방법
-- ============================================================
-- docker ps --MySQL 컨테이너 이름 확인 --> docker-mysql
--
-- docker exec -i docker-mysql-1 mysql -u application -p loopers < docs/scripts/fixture/product_performance_100k.sql
-- or
-- docker exec -it eb4a0a6d7bda bash
-- ============================================================
-- SQL 파일 실행
-- ============================================================
-- 파일 복사
-- docker cp docs/scripts/fixture/product_performance_100k.sql eb4a0a6d7bda:/tmp/product_performance_100k.sql
-- 컨테이너 접속
-- docker exec -it eb4a0a6d7bda bash
-- mysql 접속
-- mysql -u application -p loopers
-- SQL 실행 (비번 입력)
-- source /tmp/product_performance_100k.sql

-- ============================================================
-- 상품 목록 조회 성능 테스트용 더미 데이터
-- 총 100,000건 (Brand 20개, Product 100,000개, Stock 100,000개)
-- ============================================================

-- 1. 기존 데이터 정리 (필요 시 주석 해제)
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE stock;
-- TRUNCATE TABLE product;
-- TRUNCATE TABLE brand;
-- SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 2. Brand 20건 INSERT
-- ============================================================
INSERT INTO brand (name, status, created_at, updated_at)
VALUES
  ('나이키',        'ACTIVE', NOW(), NOW()),
  ('아디다스',      'ACTIVE', NOW(), NOW()),
  ('뉴발란스',      'ACTIVE', NOW(), NOW()),
  ('푸마',          'ACTIVE', NOW(), NOW()),
  ('리복',          'ACTIVE', NOW(), NOW()),
  ('언더아머',      'ACTIVE', NOW(), NOW()),
  ('컨버스',        'ACTIVE', NOW(), NOW()),
  ('반스',          'ACTIVE', NOW(), NOW()),
  ('필라',          'ACTIVE', NOW(), NOW()),
  ('살로몬',        'ACTIVE', NOW(), NOW()),
  ('아식스',        'ACTIVE', NOW(), NOW()),
  ('미즈노',        'ACTIVE', NOW(), NOW()),
  ('스케쳐스',      'ACTIVE', NOW(), NOW()),
  ('크록스',        'ACTIVE', NOW(), NOW()),
  ('버켄스탁',      'ACTIVE', NOW(), NOW()),
  ('로컬브랜드A',   'ACTIVE', NOW(), NOW()),
  ('로컬브랜드B',   'ACTIVE', NOW(), NOW()),
  ('로컬브랜드C',   'ACTIVE', NOW(), NOW()),
  ('로컬브랜드D',   'ACTIVE', NOW(), NOW()),
  ('로컬브랜드E',   'ACTIVE', NOW(), NOW());

-- ============================================================
-- 3. Product 생성 프로시저
-- ============================================================
DROP PROCEDURE IF EXISTS insert_products;

DELIMITER $$

CREATE PROCEDURE insert_products()
BEGIN
  DECLARE i INT DEFAULT 1;

  -- 카테고리 10종
  DECLARE categories JSON DEFAULT JSON_ARRAY(
    '런닝화', '농구화', '축구화', '캐주얼화', '샌들',
    '부츠', '슬리퍼', '트레킹화', '스니커즈', '로퍼'
  );

  -- 소재 5종
  DECLARE materials JSON DEFAULT JSON_ARRAY(
    '레더', '캔버스', '메쉬', '스웨이드', '니트'
  );

  -- 설명 5종
  DECLARE descriptions JSON DEFAULT JSON_ARRAY(
    '편안한 착화감과 스타일을 동시에.',
    '일상과 스포츠를 위한 최적의 선택.',
    '경량 소재로 제작된 고성능 신발.',
    '트렌디한 디자인과 내구성을 겸비.',
    '발을 편안하게 감싸는 프리미엄 제품.'
  );

  WHILE i <= 100000 DO

    -- brand_id 분포:
    --   i 1~50000   → brand_id 1~5  (각 10,000건)
    --   i 50001~80000 → brand_id 6~10 (각 3,000건, 남은 건 6~10으로 분산)
    --   나머지        → brand_id 11~20
    SET @brand_id = CASE
      WHEN i <= 10000 THEN 1
      WHEN i <= 20000 THEN 2
      WHEN i <= 30000 THEN 3
      WHEN i <= 40000 THEN 4
      WHEN i <= 50000 THEN 5
      WHEN i <= 53000 THEN 6
      WHEN i <= 56000 THEN 7
      WHEN i <= 59000 THEN 8
      WHEN i <= 62000 THEN 9
      WHEN i <= 65000 THEN 10
      WHEN i <= 68000 THEN 11
      WHEN i <= 71000 THEN 12
      WHEN i <= 74000 THEN 13
      WHEN i <= 77000 THEN 14
      WHEN i <= 80000 THEN 15
      WHEN i <= 82000 THEN 16
      WHEN i <= 84000 THEN 17
      WHEN i <= 88000 THEN 18
      WHEN i <= 94000 THEN 19
      ELSE 20
    END;

    SET @category  = JSON_UNQUOTE(JSON_EXTRACT(categories,  CONCAT('$[', (i - 1) MOD 10, ']')));
    SET @material  = JSON_UNQUOTE(JSON_EXTRACT(materials,   CONCAT('$[', (i - 1) MOD 5,  ']')));
    SET @name      = CONCAT(@category, ' ', @material, ' ', i);

    -- 가격: 1,000 ~ 1,000,000 (1,000 단위)
    SET @price = 1000 + ((i - 1) MOD 1000) * 999;

    -- 상태 분포: ACTIVE 60%, OUT_OF_STOCK 20%, PENDING 12%, INACTIVE 8%
    SET @status = CASE
      WHEN i MOD 100 < 60 THEN 'ACTIVE'
      WHEN i MOD 100 < 80 THEN 'OUT_OF_STOCK'
      WHEN i MOD 100 < 92 THEN 'PENDING'
      ELSE 'INACTIVE'
    END;

    -- 좋아요 수: 0 ~ 9,999 롱테일 분포
    SET @like_count = ((i - 1) MOD 10000);

    -- 생성일: 최근 2년(730일) 범위, 시간도 다양하게
    SET @created_at = NOW()
      - INTERVAL ((i - 1) MOD 730) DAY
      - INTERVAL ((i - 1) MOD 24) HOUR;

    -- 썸네일: 30% NULL, 70% 더미 URL
    SET @thumbnail = CASE
      WHEN i MOD 10 < 3 THEN NULL
      ELSE CONCAT('https://cdn.example.com/products/', i, '.jpg')
    END;

    SET @description = JSON_UNQUOTE(JSON_EXTRACT(descriptions, CONCAT('$[', (i - 1) MOD 5, ']')));

    INSERT INTO product
      (brand_id, name, price, status, like_count, thumbnail_image_url, description, created_at, updated_at)
    VALUES
      (@brand_id, @name, @price, @status, @like_count, @thumbnail, @description, @created_at, @created_at);

    -- 1,000건마다 커밋
    IF i MOD 1000 = 0 THEN
      COMMIT;
    END IF;

    SET i = i + 1;
  END WHILE;

  COMMIT;
END$$

DELIMITER ;

CALL insert_products();
DROP PROCEDURE IF EXISTS insert_products;

-- ============================================================
-- 4. Stock 생성 프로시저 (product와 1:1 대응)
-- ============================================================
DROP PROCEDURE IF EXISTS insert_stocks;

DELIMITER $$

CREATE PROCEDURE insert_stocks()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_product_id BIGINT;
  DECLARE v_min_id BIGINT;

  SELECT MIN(id) INTO v_min_id FROM product;

  WHILE i <= 100000 DO
    SET v_product_id = v_min_id + i - 1;

    -- 수량 분포: 0 ~ 9,999 다양하게
    SET @quantity = (i - 1) MOD 10000;

    INSERT INTO stock (product_id, quantity, created_at, updated_at)
    VALUES (v_product_id, @quantity, NOW(), NOW());

    IF i MOD 1000 = 0 THEN
      COMMIT;
    END IF;

    SET i = i + 1;
  END WHILE;

  COMMIT;
END$$

DELIMITER ;

CALL insert_stocks();
DROP PROCEDURE IF EXISTS insert_stocks;

-- ============================================================
-- 5. 결과 확인
-- ============================================================

-- 건수 확인
SELECT COUNT(*) AS product_count FROM product;
SELECT COUNT(*) AS stock_count   FROM stock;

-- 상태 분포
SELECT status, COUNT(*) AS cnt
FROM product
GROUP BY status
ORDER BY cnt DESC;

-- 브랜드별 분포
SELECT brand_id, COUNT(*) AS cnt
FROM product
GROUP BY brand_id
ORDER BY brand_id;

-- 가격 분포 (100,000원 단위 구간)
SELECT
  FLOOR(price / 100000) * 100000 AS price_range_from,
  COUNT(*) AS cnt
FROM product
GROUP BY price_range_from
ORDER BY price_range_from;


-- 실행 방법
--
--   mysql -u application -p application < docs/dumydata/product_performance_100k.sql
