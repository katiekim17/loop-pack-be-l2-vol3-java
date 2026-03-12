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
> 실행 조건: VU 50, 30s / 데이터: 81,000건

| UC | median (ms) | avg (ms) | p95 (ms) | p99 (ms) | rps |
|----|-------------|----------|----------|----------|-----|
| UC-1 | 5807 | 5573 | 8426 | 10512 | 9.4 |
| UC-2 | 5877 | 5764 | 8762 | 9175 | 9.4 |
| UC-3 | 4552 | 4492 | 6978 | 7417 | 9.4 |
| UC-4 | 4182 | 4261 | 6647 | 7184 | 9.4 |
| UC-5 | 3748 | 3706 | 6421 | 6960 | 9.4 |

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

| UC | median (ms) | avg (ms) | p95 (ms) | p99 (ms) | rps |
|----|-------------|----------|----------|----------|-----|
| UC-1 | 5049 | 5139 | 8397 | 8891 | 13.7 |
| UC-2 | 5978 | 6082 | 8991 | 10280 | 13.7 |
| UC-3 | 2043 | 2089 | 3433 | 4318 | 13.7 |
| UC-4 | 1715 | 1894 | 3685 | 4437 | 13.7 |
| UC-5 | 1352 | 1574 | 3115 | 4420 | 13.7 |

---

## 전후 비교 요약

| UC | 개선 전 p95 | 개선 후 p95 | p95 개선율 | 개선 전 p99 | 개선 후 p99 | p99 개선율 |
|----|-------------|-------------|------------|-------------|-------------|------------|
| UC-1 | 8426ms | 8397ms | 0% | 10512ms | 8891ms | 15% |
| UC-2 | 8762ms | 8991ms | -3% | 9175ms | 10280ms | -12% |
| UC-3 | 6978ms | 3433ms | 51% | 7417ms | 4318ms | 42% |
| UC-4 | 6647ms | 3685ms | 45% | 7184ms | 4437ms | 38% |
| UC-5 | 6421ms | 3115ms | 51% | 6960ms | 4420ms | 37% |

---

## 분석 결론

### 브랜드 필터 (UC-3 ~ UC-5): 인덱스 효과 큼

`brand_id + status` 복합 인덱스가 적용되어 Table scan → Index range scan으로 전환.
읽는 행 수가 81,000 → 일부로 감소하여 p95 기준 **45~51% 개선**, p99 기준 **37~42% 개선**.

전체 처리량(rps)도 9.4 → 13.7로 **약 46% 향상**.

### 전체 조회 (UC-1, UC-2): 인덱스 효과 없음

`status IN ('ACTIVE', 'OUT_OF_STOCK')` 조건이 전체 데이터의 대부분을 매칭하므로
MySQL 옵티마이저가 인덱스 대신 풀스캔을 선택. 인덱스를 추가했음에도 Table scan 유지.

UC-2(좋아요순)는 오히려 p95 -3%, p99 -12%로 소폭 악화 — 인덱스 메타데이터 오버헤드로 추정.

추가 개선이 필요하다면 Redis 캐시 레이어 도입을 고려할 수 있음.

---

## Step 7. ConcurrentHashMap 캐시 적용 (Cache-Aside)

> 실행 조건: VU 50, 30s / 데이터: 81,000건
> Spring `ConcurrentMapCacheManager` + `@Cacheable` 적용 (UC 전체)

### k6 결과

| UC | median (ms) | avg (ms) | p95 (ms) | p99 (ms) | rps |
|----|-------------|----------|----------|----------|-----|
| UC-1 | 1.7 | 124 | 40 | 4002 | 271 |
| UC-2 | 1.7 | 132 | 22 | 4992 | 271 |
| UC-3 | 1.7 | 50 | 15 | 2166 | 271 |
| UC-4 | 1.6 | 46 | 19 | 1851 | 271 |
| UC-5 | 1.6 | 54 | 16 | 2008 | 271 |

### 인덱스 없음 대비 개선율 (avg 기준)

| UC | 캐시 없음 avg | 캐시 avg | 개선율 |
|----|-------------|---------|--------|
| UC-1 | 5573ms | 124ms | 97% |
| UC-2 | 5764ms | 132ms | 97% |
| UC-3 | 4492ms | 50ms | 98% |
| UC-4 | 4261ms | 46ms | 98% |
| UC-5 | 3706ms | 54ms | 98% |

rps: 9.4 → **271** (약 29배 향상)

---

### ConcurrentHashMap 캐시의 문제점 실측

#### 1. Cache Stampede (p99 급등)

**현상**: p95 = 40ms인데 p99 = 4,002ms → 100배 차이

캐시 히트 시 응답은 수 ms 수준으로 매우 빠르지만, 캐시 미스가 발생하는 순간 여러 스레드가
동시에 DB 쿼리를 실행한다. `ConcurrentMapCacheManager`는 중복 요청을 막는 Lock 없이
그냥 통과시키기 때문에 순간적으로 DB 부하가 집중된다.

```
p95:  40ms  ← 대부분의 요청은 캐시 히트
p99: 4002ms ← 캐시 미스 순간 DB 풀스캔 중첩
```

#### 2. Stale 데이터

**현상**: `@CacheEvict` 없이는 데이터 변경이 캐시에 반영되지 않음

```java
// 이 코드만으로는 캐시가 무효화되지 않음
productService.updateProduct(id, request);

// GET /api/v1/products → 여전히 변경 전 데이터 반환
```

캐시 키(`brandId_sort_page_size`)가 동일하면 DB 변경과 무관하게
이전 응답을 계속 반환한다. 서버를 재시작하기 전까지 stale 상태가 유지됨.

#### 3. TTL 없음

**현상**: `ConcurrentMapCacheManager`는 만료 정책 자체가 없음

```java
// TTL 설정 방법이 없음
new ConcurrentMapCacheManager("productList");
// → 한 번 올라간 데이터는 서버 종료 전까지 영원히 캐시에 존재
```

메모리에 계속 쌓이고, 데이터가 아무리 오래되어도 자동 갱신이 없다.

#### 4. 분산 환경 불가

각 서버가 독립된 JVM 메모리에 캐시를 보관하므로, 서버1에서 캐시가 만들어져도
서버2는 그 캐시를 공유하지 못한다. 트래픽이 로드밸런서를 통해 분산되면
캐시가 있는 서버는 빠르게 응답하지만, 캐시가 없는 서버는 매번 DB를 조회한다.

또한 서버1에서 데이터가 변경되어 `@CacheEvict`로 캐시를 지워도,
서버2의 캐시는 그대로 남아 stale 데이터를 계속 반환하는 불일치 상태가 된다.

> 실측 시도: Java 버전 불일치(서버2가 Java 20으로 기동)로 인해 에러가 발생하여
> 수치로 검증하지 못했으나, 구조적으로 캐시가 공유되지 않음은 코드로 확인 가능.
>
> `ConcurrentMapCacheManager`는 JVM 힙 메모리(`ConcurrentHashMap`)에 데이터를 저장한다.
> 두 서버는 별개의 JVM 프로세스이므로 메모리를 공유하지 않는다.
> 즉, 서버1의 캐시 저장/삭제는 서버2에 어떤 영향도 주지 않는다.

#### 5. 모니터링 불가

캐시 히트율, 미스율, 메모리 사용량 등 어떤 지표도 관측할 수 없다.
캐시가 실제로 동작하는지조차 로그 없이는 알 수 없음.

---

### 결론

| 항목 | ConcurrentHashMap | Redis |
|------|:-----------------:|:-----:|
| 성능 (캐시 히트 시) | ✅ 빠름 | ✅ 빠름 |
| TTL 설정 | ❌ 불가 | ✅ 가능 |
| 분산 환경 | ❌ 불가 | ✅ 가능 |
| Stale 방지 | ❌ 수동 evict 필요 | ✅ TTL 자동 만료 |
| 직렬화 | ❌ 불필요 (JVM 내) | ✅ 필요 (네트워크) |
| 모니터링 | ❌ 불가 | ✅ 가능 |
| Cache Stampede 방지 | ❌ 없음 | ❌ 별도 구현 필요 |

ConcurrentHashMap 캐시는 단일 서버 + 소규모 트래픽에서는 효과적이나,
운영 환경에서는 TTL, 분산, 모니터링을 지원하는 **Redis 캐시 도입이 필요**.
