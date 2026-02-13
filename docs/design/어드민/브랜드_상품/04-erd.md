# ERD (Entity Relationship Diagram)

## 1. 전체 데이터베이스 스키마

### 설계 의도
- **참조 무결성**: FK 제약으로 데이터 일관성 보장
- **성능 최적화**: 조회 패턴에 맞는 인덱스 설계
- **확장성**: 파티셔닝, 샤딩 가능한 구조

### 특히 봐야 할 포인트
1. **products.brand_id**: FK with ON DELETE RESTRICT (어플리케이션에서 상태 변경으로 처리)
2. **product_histories**: FK 없음 (느슨한 결합, 스냅샷 독립성)
3. **product_likes**: Unique 제약 (customer_id, product_id)

```mermaid
erDiagram
    brands ||--o{ products : "has many"
    products ||--o{ product_histories : "tracks changes of"
    products ||--o{ product_likes : "liked by customers"
    
    brands {
        bigint id PK "자동 증가"
        varchar(100) name UK "브랜드명 (유니크)"
        text description "브랜드 설명"
        varchar(500) logo_url "로고 이미지 URL"
        varchar(20) status "ACTIVE, INACTIVE, PENDING, SCHEDULED"
        timestamp created_at "등록 시각"
        timestamp updated_at "수정 시각"
        varchar(100) created_by "등록자 (LDAP ID)"
    }
    
    products {
        bigint id PK "자동 증가"
        bigint brand_id FK "브랜드 ID (brands.id)"
        varchar(200) name "상품명"
        text description "상품 설명"
        decimal(15,2) price "가격"
        varchar(10) currency "통화 (KRW, USD 등)"
        int stock_quantity "재고 수량"
        varchar(20) status "ACTIVE, INACTIVE, OUT_OF_STOCK 등"
        varchar(500) image_url "상품 이미지 URL"
        timestamp created_at "등록 시각"
        timestamp updated_at "수정 시각"
        varchar(100) created_by "등록자 (LDAP ID)"
    }
    
    product_histories {
        bigint id PK "자동 증가"
        bigint product_id "상품 ID (products.id)"
        int version "버전 번호 (1부터 시작)"
        bigint brand_id "스냅샷 당시 브랜드 ID"
        varchar(200) name "스냅샷 당시 상품명"
        text description "스냅샷 당시 설명"
        decimal(15,2) price "스냅샷 당시 가격"
        varchar(10) currency "스냅샷 당시 통화"
        int stock_quantity "스냅샷 당시 재고"
        varchar(20) status "스냅샷 당시 상태"
        varchar(500) image_url "스냅샷 당시 이미지"
        timestamp changed_at "변경 시각"
        varchar(100) changed_by "변경자 (LDAP ID)"
    }
    
    product_likes {
        bigint id PK "자동 증가"
        bigint customer_id "고객 ID"
        bigint product_id FK "상품 ID (products.id)"
        timestamp created_at "좋아요 누른 시각"
    }
```

---

## 2. 테이블별 상세 스키마

### 2.1 brands 테이블

```sql
CREATE TABLE brands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 설명**:
- `name`: 브랜드명은 중복 불가 (Unique 제약)
- `status`: 기본값 PENDING (승인 후 ACTIVE)
- `created_by`: LDAP ID 저장

**인덱스 전략**:
- `idx_status`: 상태별 조회 빈번 (고객 API는 ACTIVE만 필터링)
- `idx_created_at`: 최신 등록 브랜드 조회 시 사용

---

### 2.2 products 테이블

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) 
        REFERENCES brands(id) ON DELETE RESTRICT,
    
    INDEX idx_brand_id (brand_id),
    INDEX idx_status (status),
    INDEX idx_brand_status (brand_id, status),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 설명**:
- `brand_id`: 브랜드 FK (ON DELETE RESTRICT - 어플리케이션에서 처리)
- `price`, `currency`: 금액은 통화와 함께 저장
- `stock_quantity`: 재고 수량 (0 이하면 OUT_OF_STOCK 상태로 변경)

**인덱스 전략**:
- `idx_brand_id`: 브랜드별 상품 조회
- `idx_status`: 상태별 필터링 (ACTIVE, OUT_OF_STOCK)
- `idx_brand_status`: 브랜드+상태 복합 조회 최적화
- `idx_created_at`: 신상품 정렬

**ON DELETE RESTRICT 이유**:
- 브랜드 삭제 시 DB 레벨에서 막지 않고, 어플리케이션에서 상태 변경으로 처리
- 실수로 브랜드 물리 삭제 시도 시 에러 발생 (데이터 보호)

---

### 2.3 product_histories 테이블

```sql
CREATE TABLE product_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    version INT NOT NULL,
    brand_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    stock_quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image_url VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100) NOT NULL,
    
    UNIQUE KEY uk_product_version (product_id, version),
    INDEX idx_product_id_changed_at (product_id, changed_at DESC),
    INDEX idx_changed_at (changed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(changed_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**컬럼 설명**:
- `product_id`: 원본 상품 ID (FK 없음 - 스냅샷 독립성)
- `version`: 변경 버전 (1부터 시작, 자동 증가)
- `brand_id`, `name`, `price` 등: 변경 시점의 스냅샷

**Unique 제약**:
- `uk_product_version`: 동일 상품의 동일 버전 중복 방지

**인덱스 전략**:
- `idx_product_id_changed_at`: 상품 이력 조회 (최신순 정렬)
- `idx_changed_at`: 전체 변경 이력 조회

**파티셔닝 전략**:
- 연도별 파티션으로 이력 테이블 증가 대비
- 오래된 이력은 별도 아카이빙 가능

**FK 없는 이유**:
- Product 삭제 시에도 이력은 보존 (감사 추적)
- 스냅샷은 독립적으로 존재

---

### 2.4 product_likes 테이블

```sql
CREATE TABLE product_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_product_likes_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_customer_product (customer_id, product_id),
    INDEX idx_product_id (product_id),
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 설명**:
- `customer_id`: 고객 ID (나중에 customers 테이블과 FK 연결 가능)
- `product_id`: 상품 FK (ON DELETE CASCADE - 상품 삭제 시 좋아요도 삭제)

**Unique 제약**:
- `uk_customer_product`: 동일 고객이 동일 상품에 중복 좋아요 방지

**인덱스 전략**:
- `idx_product_id`: 상품별 좋아요 수 집계
- `idx_customer_id`: 고객별 좋아요 목록 조회

**ON DELETE CASCADE 이유**:
- 상품이 삭제(비활성화)되면 좋아요도 의미 없음
- 좋아요 이력은 별도 추적 불필요 (현재 상태만 중요)

---

## 3. 인덱스 전략 상세

### 3.1 조회 패턴별 인덱스

| 조회 패턴 | 사용 인덱스 | 설명 |
|----------|-----------|------|
| 활성 브랜드 목록 | `brands.idx_status` | WHERE status = 'ACTIVE' |
| 브랜드별 상품 목록 | `products.idx_brand_status` | WHERE brand_id = ? AND status IN (?, ?) |
| 상품 이력 조회 | `product_histories.idx_product_id_changed_at` | WHERE product_id = ? ORDER BY changed_at DESC |
| 상품별 좋아요 수 | `product_likes.idx_product_id` | COUNT(*) WHERE product_id = ? |
| 고객별 좋아요 목록 | `product_likes.idx_customer_id` | WHERE customer_id = ? |

### 3.2 복합 인덱스 최적화

```sql
-- 브랜드별 활성 상품 조회에 최적화
CREATE INDEX idx_brand_status ON products (brand_id, status);

-- 상품 이력 최신순 조회에 최적화
CREATE INDEX idx_product_id_changed_at ON product_histories (product_id, changed_at DESC);
```

**복합 인덱스 사용 이유**:
- `idx_brand_status`: 브랜드별 + 상태별 필터링 동시 최적화
- 인덱스 순서: `brand_id` (등호 조건) → `status` (IN 조건)

---

## 4. 데이터 정합성 제약

### 4.1 FK 제약 정리

| 테이블 | FK | 참조 | ON DELETE | 이유 |
|-------|----|----|-----------|------|
| products | brand_id | brands(id) | RESTRICT | 브랜드 물리 삭제 방지 (상태로 관리) |
| product_likes | product_id | products(id) | CASCADE | 상품 삭제 시 좋아요도 삭제 |

### 4.2 Unique 제약 정리

| 테이블 | Unique 제약 | 의미 |
|-------|-----------|------|
| brands | name | 브랜드명 중복 방지 |
| product_histories | (product_id, version) | 동일 상품의 버전 중복 방지 |
| product_likes | (customer_id, product_id) | 중복 좋아요 방지 |

### 4.3 Check 제약 (향후 추가 가능)

```sql
-- 가격은 0 이상
ALTER TABLE products ADD CONSTRAINT chk_price_positive 
    CHECK (price >= 0);

-- 재고는 음수 불가
ALTER TABLE products ADD CONSTRAINT chk_stock_non_negative 
    CHECK (stock_quantity >= 0);

-- 상태는 허용된 값만
ALTER TABLE brands ADD CONSTRAINT chk_brand_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SCHEDULED'));

ALTER TABLE products ADD CONSTRAINT chk_product_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SCHEDULED', 'OUT_OF_STOCK'));
```

**주의**: MySQL 8.0.16 이상에서만 Check 제약 지원

---

## 5. 브랜드 비활성화 시 연쇄 처리 SQL

### 5.1 어플리케이션에서 실행할 쿼리

```sql
-- 트랜잭션 A: 브랜드 상태 변경
UPDATE brands 
SET status = 'INACTIVE', updated_at = NOW() 
WHERE id = ? AND status = 'ACTIVE';

-- 트랜잭션 B: 상품 일괄 비활성화 (이벤트 리스너에서 실행)
UPDATE products 
SET status = 'INACTIVE', updated_at = NOW() 
WHERE brand_id = ? AND status = 'ACTIVE';

-- 트랜잭션 B: 각 상품의 스냅샷 저장
INSERT INTO product_histories 
    (product_id, version, brand_id, name, description, price, currency, 
     stock_quantity, status, image_url, changed_at, changed_by)
SELECT 
    id, 
    (SELECT IFNULL(MAX(version), 0) + 1 FROM product_histories ph WHERE ph.product_id = p.id),
    brand_id, name, description, price, currency, 
    stock_quantity, status, image_url, NOW(), 'SYSTEM'
FROM products p
WHERE brand_id = ?;
```

### 5.2 고객 조회 시 필터링 쿼리

```sql
-- 브랜드가 비활성이면 상품도 제외
SELECT p.*, b.name as brand_name, COUNT(pl.id) as like_count
FROM products p
INNER JOIN brands b ON p.brand_id = b.id
LEFT JOIN product_likes pl ON p.id = pl.product_id
WHERE b.status = 'ACTIVE'
  AND p.status IN ('ACTIVE', 'OUT_OF_STOCK')
  AND (? IS NULL OR p.brand_id = ?)
GROUP BY p.id
ORDER BY p.created_at DESC
LIMIT ? OFFSET ?;
```

---

## 6. 확장성 고려사항

### 6.1 파티셔닝 전략
- **product_histories**: 연도별 파티셔닝으로 이력 테이블 크기 관리
- **product_likes**: 고객 ID 기반 해시 파티셔닝 (나중에 샤딩 가능)

### 6.2 인덱스 유지보수
- **주기적 통계 갱신**: `ANALYZE TABLE products;`
- **사용하지 않는 인덱스 제거**: 쿼리 로그 분석 후

### 6.3 읽기 성능 최적화
- **좋아요 수 캐싱**: Redis에 `product:{id}:like_count` 저장
- **읽기 전용 레플리카**: 고객 조회는 레플리카로 분산

---

## 7. ERD 해석 가이드

### 핵심 설계 원칙
1. **정규화**: 중복 데이터 최소화 (브랜드 정보는 brands에만)
2. **참조 무결성**: FK 제약으로 데이터 일관성 보장
3. **성능 최적화**: 조회 패턴에 맞는 인덱스 설계

### 이 구조에서 특히 봐야 할 포인트
- **products.brand_id**: ON DELETE RESTRICT로 물리 삭제 방지
- **product_histories**: FK 없음 (스냅샷 독립성)
- **product_likes**: Unique 제약으로 중복 방지, ON DELETE CASCADE

### 잠재 리스크
- **product_histories 증가**: 파티셔닝으로 완화
- **좋아요 수 집계 비용**: COUNT(*) 대신 캐시 활용
- **브랜드-상품 조인**: 인덱스 최적화 필수

---

## 8. 데이터 마이그레이션 스크립트

### 8.1 초기 테이블 생성 순서
```sql
-- 1. brands 테이블 생성
CREATE TABLE brands (...);

-- 2. products 테이블 생성 (brands FK 필요)
CREATE TABLE products (...);

-- 3. product_histories 테이블 생성 (FK 없음)
CREATE TABLE product_histories (...);

-- 4. product_likes 테이블 생성 (products FK 필요)
CREATE TABLE product_likes (...);
```

### 8.2 샘플 데이터 INSERT
```sql
-- 브랜드 샘플 데이터
INSERT INTO brands (name, description, logo_url, status, created_by) VALUES
('Nike', 'Just Do It', 'https://example.com/nike-logo.png', 'ACTIVE', 'admin'),
('Adidas', 'Impossible is Nothing', 'https://example.com/adidas-logo.png', 'ACTIVE', 'admin');

-- 상품 샘플 데이터
INSERT INTO products (brand_id, name, description, price, currency, stock_quantity, status, created_by) VALUES
(1, 'Air Max 90', 'Classic sneakers', 150000, 'KRW', 100, 'ACTIVE', 'admin'),
(1, 'Air Force 1', 'Iconic shoes', 120000, 'KRW', 0, 'OUT_OF_STOCK', 'admin'),
(2, 'Ultraboost', 'Running shoes', 180000, 'KRW', 50, 'ACTIVE', 'admin');

-- 초기 스냅샷 저장
INSERT INTO product_histories 
    (product_id, version, brand_id, name, description, price, currency, stock_quantity, status, changed_at, changed_by)
SELECT id, 1, brand_id, name, description, price, currency, stock_quantity, status, created_at, created_by
FROM products;
```

---

## 정리

이 ERD는 다음을 보장합니다:

1. **데이터 정합성**: FK 제약, Unique 제약으로 일관성 유지
2. **확장성**: 파티셔닝, 인덱스 최적화로 성능 확보
3. **감사 추적**: product_histories로 모든 변경 이력 보관
4. **유연성**: 상태 관리로 물리 삭제 없이 비활성화 처리

**다음 단계**: JPA Entity 설계 시 이 ERD를 기반으로 매핑