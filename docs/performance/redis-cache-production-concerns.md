# Redis 캐시 운영 고려사항

> 상품 목록 조회 Redis 캐시 도입 이후, 실제 운영 환경(특히 트래픽 급증 시)에서 발생할 수 있는 문제와 대응 전략을 정리한다.

---

## 현재 구조 요약

```
Client → Spring App (@Cacheable) → Redis → (miss 시) MySQL
```

- 캐시 키: `productList::null_latest_0_20`
- TTL: 5분
- 직렬화: `Jackson2JsonRedisSerializer<ProductListPage>`
- Redis: Master 1대 + Replica 1대

---

## 문제 1. Cache Stampede

### 현상

TTL이 만료되는 순간, 대기 중이던 수천 개의 요청이 **동시에 캐시 미스**를 인식하고 DB로 쿼리를 보낸다.

```
평소 (요청 100/s):
  TTL 만료 → 1~2개 스레드가 DB 조회 → 캐시 재적재 → 정상

크리스마스 (요청 5000/s):
  TTL 만료 → 수천 스레드가 동시에 DB 조회
           → DB 커넥션 풀 고갈
           → Connection timeout
           → 클라이언트 재시도 (Thundering Herd)
           → DB 과부하 → 서비스 장애
```

### 재현 시나리오

1. k6로 VU 500, 60s 부하 테스트 실행
2. Redis에서 해당 키 강제 삭제: `redis-cli DEL "productList::null_latest_0_20"`
3. 이 순간 p99 응답 시간이 급등하는 것을 확인

### 대응 방안

#### 방법 A. TTL Jitter (즉시 적용 가능)

만료 시간을 분산시켜 동시 만료를 방지한다.

```java
// CacheConfig.java
.entryTtl(Duration.ofSeconds(270 + ThreadLocalRandom.current().nextInt(60)))
// 4분 30초 ~ 5분 30초 사이 랜덤 TTL
```

- 장점: 구현 간단, 리스크 거의 없음
- 단점: 근본 해결이 아님. 트래픽이 매우 높으면 여전히 발생 가능

#### 방법 B. Mutex Lock (분산 락)

캐시 미스 시 단 하나의 스레드만 DB 조회를 허용하고, 나머지는 대기 또는 stale 값을 반환한다.

```java
// Redisson 활용 예시 (개념 코드)
public ProductListPage getProductList(...) {
    String lockKey = "lock:productList:" + cacheKey;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 캐시 확인
        ProductListPage cached = cache.get(cacheKey);
        if (cached != null) return cached;

        // 락 획득 시도 (최대 3초 대기, 5초 후 자동 해제)
        if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
            try {
                // 락 획득 후 다시 캐시 확인 (Double-Checked Locking)
                cached = cache.get(cacheKey);
                if (cached != null) return cached;

                // DB 조회 및 캐시 적재
                ProductListPage result = queryFromDB(...);
                cache.put(cacheKey, result);
                return result;
            } finally {
                lock.unlock();
            }
        } else {
            // 락 획득 실패 → stale 캐시 또는 빈 응답 반환
            return cache.getStale(cacheKey);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
    }
}
```

- 장점: Stampede 완전 방지
- 단점: 코드 복잡도 증가, Redisson 의존성 추가

#### 방법 C. PER (Probabilistic Early Recomputation)

만료 직전에 확률적으로 미리 캐시를 갱신한다. 만료 시점이 가까울수록 갱신 확률이 높아진다.

```
현재 시각 t, TTL 남은 시간 δ, 재계산 비용 β, 랜덤 인수 X

if (t - β * log(X)) > (만료 시각 - δ):
    캐시 갱신
```

- 장점: 락 없이 Stampede 방지, 구현이 비교적 단순
- 단점: 일부 요청이 불필요하게 DB 조회할 수 있음

---

## 문제 2. Cache Invalidation 전략 부재

### 현상

상품 추가/수정/삭제 후에도 TTL이 남아있으면 5분간 구버전 데이터가 노출된다.

```
크리스마스 00:00  한정 상품 등록
크리스마스 00:04  유저에게 여전히 이전 목록 노출 → 비즈니스 손실
```

### 대응 방안

#### 방법 A. @CacheEvict (즉시 적용 가능)

```java
// ProductService.java 또는 ProductFacade.java
@CacheEvict(cacheNames = "productList", allEntries = true)
public void createProduct(...) { ... }

@CacheEvict(cacheNames = "productList", allEntries = true)
public void updateProduct(...) { ... }

@CacheEvict(cacheNames = "productList", allEntries = true)
public void deleteProduct(...) { ... }
```

- 장점: 구현 간단
- 단점: 상품 변경 시 전체 캐시 삭제 → 일시적 DB 부하 증가

#### 방법 B. 이벤트 기반 캐시 무효화

```
Product 변경 → Kafka 이벤트 발행 → Commerce-Streamer 소비 → 캐시 무효화
```

- 장점: 서비스 간 결합도 낮음, 정확한 무효화
- 단점: 구조 복잡도 증가, 이벤트 처리 지연 가능

---

## 문제 3. Redis 단일 장애점 (SPOF)

### 현상

현재 Master 장애 시 Replica 승격에 수십 초가 소요되며, 그동안 캐시 서비스가 불가능하다.

```
Master 장애
  → Replica 승격 (10~30초)
  → 그 동안 모든 캐시 미스
  → DB에 전체 트래픽 직격
  → 크리스마스 성수기에 발생하면 치명적
```

### 대응 방안

#### 방법 A. Redis Sentinel

자동 failover를 지원하며, Spring Data Redis에서 기본 지원한다.

```yaml
# redis.yml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes:
        - sentinel-1:26379
        - sentinel-2:26379
        - sentinel-3:26379
```

#### 방법 B. Circuit Breaker (Resilience4j)

Redis 장애 시 캐시를 우회하여 DB에서 직접 조회하도록 fallback 처리.

```java
@CircuitBreaker(name = "redis", fallbackMethod = "getProductListFallback")
@Cacheable(cacheNames = "productList", ...)
public ProductListPage getProductList(...) { ... }

public ProductListPage getProductListFallback(Long brandId, String sort, int page, int size, Exception e) {
    // 캐시 없이 DB 직접 조회
    return queryFromDB(brandId, sort, page, size);
}
```

- 장점: Redis 장애 시 서비스 지속 가능
- 단점: DB 부하 증가, Resilience4j 의존성 추가

---

## 문제 4. Hot Key 집중

### 현상

상품 목록 첫 페이지(`null_latest_0_20`)는 전체 요청의 80% 이상이 동일 키로 집중된다.
Redis는 single-threaded이므로, 특정 키에 요청이 몰리면 Redis 자체가 병목이 된다.

```
VU 5000 → 4000개 요청이 동일 키 → Redis 처리 지연 → 응답 시간 상승
```

### 대응 방안: 2-Layer Cache (Local + Redis)

```
Client → Local Cache (Caffeine, 30초 TTL) → Redis (5분 TTL) → MySQL
```

```java
// 개념 코드
public ProductListPage getProductList(...) {
    String key = buildKey(brandId, sort, page, size);

    // Layer 1: 로컬 캐시 (수 ms, 네트워크 없음)
    ProductListPage local = localCache.getIfPresent(key);
    if (local != null) return local;

    // Layer 2: Redis 캐시
    ProductListPage redis = redisCache.get(key);
    if (redis != null) {
        localCache.put(key, redis);
        return redis;
    }

    // Layer 3: DB
    ProductListPage result = queryFromDB(...);
    redisCache.put(key, result);
    localCache.put(key, result);
    return result;
}
```

| 레이어 | TTL | 특징 |
|--------|-----|------|
| Caffeine (Local) | 30초 | 네트워크 없음, 서버당 독립 |
| Redis | 5분 | 분산 공유, TTL/모니터링 |

- 장점: Redis 요청 수 대폭 감소, 응답 시간 최소화
- 단점: 로컬 캐시가 서버별로 분리되어 일시적 불일치 가능

---

## 문제 5. Cache Cold Start (배포 직후)

### 현상

배포/재시작 직후 모든 캐시가 비어있어 초기 트래픽이 전부 DB를 직격한다.

```
크리스마스 전날 23:55 배포
크리스마스 00:00 트래픽 급증 → 캐시 없음 → DB 스파이크
```

### 대응 방안: 캐시 Warm-up

```java
@Component
public class CacheWarmup implements ApplicationRunner {

    private final ProductFacade productFacade;

    @Override
    public void run(ApplicationArguments args) {
        // 가장 많이 조회되는 첫 3페이지 미리 적재
        for (int page = 0; page < 3; page++) {
            productFacade.getProductList(null, "latest", page, 20);
        }
        log.info("Cache warm-up 완료");
    }
}
```

---

## 문제 6. 모니터링 부재

### 현재 상태

캐시 히트율, Redis 메모리, 캐시 미스 급증을 감지할 방법이 없다.

### Micrometer 캐시 메트릭 활성화

이미 Micrometer + Prometheus가 구성되어 있으므로, 설정만 추가하면 된다.

```yaml
# application.yml
management:
  metrics:
    cache:
      instrument:
        enabled: true
```

활성화 시 다음 메트릭이 자동 수집된다:

| 메트릭 | 설명 |
|--------|------|
| `cache.gets{result="hit"}` | 캐시 히트 수 |
| `cache.gets{result="miss"}` | 캐시 미스 수 |
| `cache.puts` | 캐시 적재 수 |
| `cache.evictions` | 캐시 만료/삭제 수 |

### 권장 Grafana 알람

| 조건 | 의미 | 대응 |
|------|------|------|
| 히트율 < 80% | 캐시가 제 역할 못함 | TTL 검토, 키 전략 재검토 |
| 미스율 급등 | Stampede 의심 | 즉시 조사 |
| Redis 메모리 > 70% | OOM 위험 | eviction 정책 검토 |

---

## 우선순위 로드맵

```
Phase 1. 즉시 (서비스 영향 직결)
├── TTL Jitter 적용                  → Stampede 리스크 감소
├── @CacheEvict 추가                 → 데이터 정합성 확보
└── Micrometer 캐시 메트릭 활성화    → 히트율 가시성 확보

Phase 2. 단기 (안정성)
├── Redis Sentinel 구성              → SPOF 제거
├── Circuit Breaker (Resilience4j)   → Redis 장애 시 fallback
└── Cache Warm-up                    → 배포 직후 DB 스파이크 방지

Phase 3. 중장기 (성능 고도화)
├── 2-Layer Cache (Caffeine + Redis) → Hot key 문제 해결
├── Mutex Lock / PER                 → Stampede 완전 방지
└── 이벤트 기반 캐시 무효화          → 정확한 invalidation
```

---

## 참고: 크리스마스 장애 시나리오 전체 흐름

```
00:00  트래픽 평소의 10배 유입
00:00  Redis 캐시 히트율 95% → 잘 버팀
00:05  TTL 5분 만료 도달
00:05  수천 스레드 동시 캐시 미스 → DB 쿼리 폭발
00:05  HikariPool 커넥션 고갈 → 대기 타임아웃
00:06  클라이언트 재시도 → Thundering Herd
00:06  DB CPU 100% → 슬로우 쿼리 급증
00:07  DB 응답 없음 → Spring 에러 응답
00:07  모니터링 알람 없음 → 개발자 늦게 감지
00:15  개발자 개입, Redis 캐시 수동 적재
00:20  서비스 정상화

→ 20분 장애, 손실 발생
→ TTL Jitter 하나만 있었어도 리스크 대폭 감소
```