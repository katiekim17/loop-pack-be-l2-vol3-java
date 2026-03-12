# 상품 목록 조회 인덱스 최적화 계획

## 목표

`GET /api/v1/products` 엔드포인트에서 브랜드 필터 + 정렬 조건별 유즈케이스를 분석하여
인덱스를 적용하고, 전후 성능을 비교한다.

---

## 측정 도구

### Level 1 — 쿼리 실행 계획 (MySQL)

| 도구 | 용도 |
|------|------|
| `EXPLAIN ANALYZE` | 실제 실행 시간 + 예상 vs 실제 rows 비교 |
| `EXPLAIN FORMAT=JSON` | cost, rows 수치 트리 구조로 확인 |

### Level 2 — API 부하 테스트 (HTTP)

| 도구 | 이유 |
|------|------|
| **k6** | JS 시나리오 작성 용이, Grafana 연동 가능 |
| Grafana + Prometheus | 이미 스택에 포함 — 실시간 대시보드 |

---

## 분석 대상 유즈케이스

| # | 시나리오 | 쿼리 조건 |
|---|----------|-----------|
| UC-1 | 전체 조회 + 최신순 (기본) | `status IN (ACTIVE, OUT_OF_STOCK) ORDER BY created_at DESC` |
| UC-2 | 전체 조회 + 좋아요순 | `status IN (...) ORDER BY like_count DESC` |
| UC-3 | 브랜드 필터 + 최신순 | `brand_id = ? AND status IN (...) ORDER BY created_at DESC` |
| UC-4 | 브랜드 필터 + 좋아요순 | `brand_id = ? AND status IN (...) ORDER BY like_count DESC` |
| UC-5 | 브랜드 필터 + 가격순 | `brand_id = ? AND status IN (...) ORDER BY price ASC` |

---

## 진행 순서

```
Step 1. 더미 데이터 주입
Step 2. 인덱스 없는 상태에서 EXPLAIN ANALYZE 실행 → 결과 기록
Step 3. k6 부하 테스트 실행 → 결과 기록
Step 4. 인덱스 설계 및 적용
Step 5. 동일 조건으로 EXPLAIN ANALYZE + k6 재실행
Step 6. 전후 비교 정리
```

---

## 파일 구성

```
docs/performance/
├── index-optimization-plan.md     ← 이 파일 (계획 및 전후 비교 결과)
├── explain-queries.sql             ← EXPLAIN ANALYZE 실행 쿼리 모음
└── index-migration.sql             ← 인덱스 DDL

k6/
└── product-list.js                 ← k6 부하 테스트 스크립트
```

---

## Step 2. EXPLAIN ANALYZE 결과 (인덱스 적용 전)

> 더미 데이터 10만 건 기준으로 실행 후 결과를 아래에 기록한다.

| UC | type | key | rows (예상) | actual rows | actual time |
|----|------|-----|-------------|-------------|-------------|
| UC-1 | ALL | NULL | 92,348 | 100,000 | 82ms |
| UC-2 | ALL | NULL | 92,348 | 100,000 | 143ms |
| UC-3 | ALL | NULL | 92,348 | 100,000 | 62.9ms |
| UC-4 | ALL | NULL | 92,348 | 100,000 | 67.9ms |
| UC-5 | ALL | NULL | 92,348 | 100,000 | 65.7ms |

---

## Step 3. k6 부하 테스트 결과 (인덱스 적용 전)

> `k6/product-list.js` 실행 후 결과를 아래에 기록한다.
> 실행 조건: VU 50, 30s

| UC | avg (ms) | p95 (ms) | rps |
|----|----------|----------|-----|
| UC-1 | 1808 | 2812 | 5.6 |
| UC-2 | 2057 | 2976 | 5.6 |
| UC-3 | 1616 | 2284 | 5.6 |
| UC-4 | 1488 | 2234 | 5.6 |
| UC-5 | 1226 | 1751 | 5.6 |

---

## Step 4. 인덱스 설계

> `docs/performance/index-migration.sql` 참고

| 인덱스명 | 대상 컬럼 | 커버하는 UC |
|----------|-----------|-------------|
| `idx_product_status_created` | `(status, created_at DESC)` | UC-1 |
| `idx_product_status_like` | `(status, like_count DESC)` | UC-2 |
| `idx_product_brand_status_created` | `(brand_id, status, created_at DESC)` | UC-3 |
| `idx_product_brand_status_like` | `(brand_id, status, like_count DESC)` | UC-4 |
| `idx_product_brand_status_price` | `(brand_id, status, price ASC)` | UC-5 |

---

## Step 5. EXPLAIN ANALYZE 결과 (인덱스 적용 후)

| UC | type | key | rows (예상) | actual rows | actual time |
|----|------|-----|-------------|-------------|-------------|
| UC-1 | ALL | NULL | 92,348 | 100,000 | 94.3ms |
| UC-2 | ALL | NULL | 92,348 | 100,000 | 76.6ms |
| UC-3 | range | idx_product_brand_status_created | 12,934 | 8,000 | 14.7ms |
| UC-4 | range | idx_product_brand_status_created | 12,934 | 8,000 | 13.4ms |
| UC-5 | range | idx_product_brand_status_created | 12,934 | 8,000 | 12.8ms |

---

## Step 6. k6 부하 테스트 결과 (인덱스 적용 후)

| UC | avg (ms) | p95 (ms) | rps |
|----|----------|----------|-----|
| UC-1 | 1415 | 2210 | 10.2 |
| UC-2 | 1514 | 2163 | 10.2 |
| UC-3 | 480 | 824 | 10.2 |
| UC-4 | 448 | 801 | 10.2 |
| UC-5 | 413 | 731 | 10.2 |

---

## 전후 비교 요약

| UC | 개선 전 p95 | 개선 후 p95 | 개선율 |
|----|-------------|-------------|--------|
| UC-1 | 2812ms | 2210ms | 21% |
| UC-2 | 2976ms | 2163ms | 27% |
| UC-3 | 2284ms | 824ms | 64% |
| UC-4 | 2234ms | 801ms | 64% |
| UC-5 | 1751ms | 731ms | 58% |

---

## 분석 결론

### 브랜드 필터 (UC-3 ~ UC-5): 인덱스 효과 큼

`brand_id + status` 복합 인덱스(`idx_product_brand_status_created`)가 적용되어
Table scan → Index range scan으로 전환. 읽는 행 수가 100,000 → 8,000으로 감소하여 p95 기준 **58~64% 개선**.

전체 처리량(rps)도 5.6 → 10.2로 **약 2배 향상**.

### 전체 조회 (UC-1, UC-2): 인덱스 효과 제한적

`status IN ('ACTIVE', 'OUT_OF_STOCK')` 조건이 전체 데이터의 80%를 매칭하므로
MySQL 옵티마이저가 인덱스 대신 풀스캔을 선택. 인덱스를 추가했음에도 Table scan 유지.

추가 개선이 필요하다면 Redis 캐시 레이어 도입을 고려할 수 있음.
