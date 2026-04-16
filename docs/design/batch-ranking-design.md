# Batch Ranking 설계

## 개요

`product_metrics` 테이블을 기반으로 일간/주간/월간 랭킹을 집계하는 Spring Batch Job 설계.

---

## 1. product_metrics 테이블

하루치 상품 판매량을 1행으로 저장하는 테이블.

```sql
product_id | date       | sales_count
1          | 2026-04-15 | 100
```

### 중복 처리 전략

- 시나리오: 매일 자정에 "어제 판매량"을 집계해서 한 번 INSERT
- 배치 재실행 시 같은 `(product_id, date)` 중복 발생 가능
- 랭킹 특성상 완벽한 정확도보다 운영 편의성 우선 → **덮어쓰기(OVERWRITE)**

```sql
INSERT INTO product_metrics (product_id, date, sales_count)
VALUES (1, '2026-04-15', 100)
ON DUPLICATE KEY UPDATE sales_count = VALUES(sales_count)
```

---

## 2. Spring Batch Job 구성

### Chunk-Oriented 처리 흐름

```
Reader  → product_metrics에서 날짜(Job Parameter) 기준으로 읽기
Processor → pass-through (집계는 Writer SQL에서 처리)
Writer  → MV 테이블에 집계 결과 저장
```

### Chunk Size

- 데이터 규모에 따라 성능 테스트 후 결정
- 초기값: 1000 (이후 튜닝)

### Job 분리

| Job | 역할 | 실행 주기 |
|-----|------|-----------|
| `dailyMetricsJob` | product_metrics 집계 저장 | 매일 새벽 1시 (`0 1 * * *`) |
| `weeklyRankingJob` | mv_product_rank_weekly 갱신 | 매주 월요일 새벽 2시 (`0 2 * * 1`) |
| `monthlyRankingJob` | mv_product_rank_monthly 갱신 | 매월 1일 새벽 2시 (`0 2 1 * *`) |

- Job 분리 이유: 실행 주기가 다르고, 한 Job 실패가 다른 Job에 영향을 주지 않도록
- 실행 순서: 일간 집계 완료 후 주간/월간 MV 갱신 (시간 차이를 두어 순차 보장)

---

## 3. Materialized View 설계

MySQL 8.0은 실제 Materialized View를 지원하지 않으므로 **일반 테이블로 구현**.

### mv_product_rank_weekly

- 집계 기준: 월요일 ~ 일요일 (캘린더 주)
- `week_start` 계산: `date.with(DayOfWeek.MONDAY)`

```sql
INSERT INTO mv_product_rank_weekly (product_id, week_start, total_sales)
SELECT product_id, ?, SUM(sales_count)
FROM product_metrics
WHERE date BETWEEN ? AND ?
GROUP BY product_id
ORDER BY total_sales DESC
LIMIT 100
ON DUPLICATE KEY UPDATE total_sales = VALUES(total_sales)
```

### mv_product_rank_monthly

- 집계 기준: 해당 월 1일 ~ 말일 (캘린더 월)

```sql
INSERT INTO mv_product_rank_monthly (product_id, month, total_sales)
SELECT product_id, ?, SUM(sales_count)
FROM product_metrics
WHERE date BETWEEN ? AND ?
GROUP BY product_id
ORDER BY total_sales DESC
LIMIT 100
ON DUPLICATE KEY UPDATE total_sales = VALUES(total_sales)
```

---

## 4. Ranking API 확장

### 파라미터 설계

```
GET /api/v1/rankings?date=20260415&period=daily&size=20&page=1
```

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `date` | yyyyMMdd | 오늘 | 조회 기준 날짜 |
| `period` | daily \| weekly \| monthly | daily | 집계 기간 |
| `size` | int | 20 | 페이지 크기 |
| `page` | int | 1 | 페이지 번호 (1-based) |

- `period` 미입력 시 `daily` → 기존 클라이언트 하위 호환성 유지
- `period=weekly`일 때 `date`는 해당 주 안의 임의 날짜 → `date.with(DayOfWeek.MONDAY)`로 주 시작일 계산

### RankingPeriod Enum

```java
public enum RankingPeriod {
    DAILY, WEEKLY, MONTHLY
}
```

- `String` 대신 `enum` 사용 → 컴파일 타임 오타 방지
- Controller 입력 시 대소문자 변환 처리 필요 (`"weekly"` → `WEEKLY`)

### Repository 분리

| 저장소 | 인터페이스 | 구현 |
|--------|-----------|------|
| 일간 | `RankingRepository` | Redis ZSET |
| 주간 | `WeeklyRankingRepository` | MySQL mv_product_rank_weekly |
| 월간 | `MonthlyRankingRepository` | MySQL mv_product_rank_monthly |

- `RankingFacade`에서 `RankingPeriod`로 분기하여 각 Repository 호출

```java
public List<RankingInfo> getRankings(LocalDate date, RankingPeriod period, int page, int size) {
    return switch (period) {
        case WEEKLY -> // WeeklyRankingRepository 호출
        case MONTHLY -> // MonthlyRankingRepository 호출
        default -> // 기존 Redis RankingRepository 호출
    };
}
```

---

## 5. 미결 사항

- [ ] `RankingPeriod` enum을 `@RequestParam`에서 대소문자 무관하게 변환하는 방법