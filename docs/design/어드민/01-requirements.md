# 요구사항 명세서

## 📋 문서 정보
- **작성일**: 2026-02-12
- **버전**: 1.0
- **목적**: 브랜드 & 상품 관리 시스템 요구사항 정의

---

## 1. 문제 상황 정의

### 1.1 사용자 관점
- **어드민 사용자**: 브랜드와 상품을 등록/수정/비활성화하며 카탈로그를 관리해야 함
- **일반 고객**: 활성화된 브랜드와 상품 정보를 조회하고, 상호작용(좋아요)을 통해 관심을 표현해야 함
- **문제**: 어드민의 관리 정보와 고객에게 노출되는 정보가 다르며, 역할별 접근 제어가 필요함

### 1.2 비즈니스 관점
- **카탈로그 일관성**: 브랜드가 비활성화되면 해당 상품들도 함께 비활성화되어야 함
- **변경 이력 추적**: 상품의 모든 변경 사항(브랜드 변경 포함)을 스냅샷으로 보관하여 감사 추적이 가능해야 함
- **재고 관리**: 상품의 재고 상태를 추적하여 품절 시 고객에게 적절히 표시해야 함
- **문제**: 브랜드-상품 간 상태 동기화, 이력 관리, 재고 정합성 유지가 필요함

### 1.3 시스템 관점
- **인증/인가**: LDAP 헤더(`X-Loopers-Ldap: loopers.admin`)로 어드민을 식별하여 접근 제어
- **데이터 정합성**: 브랜드-상품 간 참조 무결성, 상태 연쇄 변경, 변경 이력 스냅샷 저장
- **확장성**: 검색/필터링, 정렬, 페이징 기능 확장 가능하도록 설계
- **문제**: 비활성화 트랜잭션 처리, 스냅샷 저장 전략, 모듈 간 책임 분리가 필요함

---

## 2. 핵심 도메인 개념

### 2.1 액터 (Actors)
- **Admin User**: 사내 어드민 시스템 사용자 (LDAP 인증)
- **Customer**: 일반 고객 (브랜드/상품 조회 및 상호작용)

### 2.2 핵심 도메인 (Core Domain)
- **Brand**: 브랜드 (제조사/판매자)
    - 상태: ACTIVE, INACTIVE, PENDING, SCHEDULED
    - 브랜드 비활성화 시 모든 상품 연쇄 비활성화

- **Product**: 상품
    - 상태: ACTIVE, INACTIVE, PENDING, SCHEDULED, OUT_OF_STOCK
    - 브랜드 변경 가능 (이력 추적)
    - 재고 소진 시 OUT_OF_STOCK 상태

- **ProductHistory**: 상품 변경 이력 (스냅샷)
    - 상품의 모든 변경 사항을 버전별로 저장
    - 특정 시점의 상품 상태 복원 가능

- **ProductLike**: 고객의 상품 좋아요
    - 고객별 좋아요 관리
    - 좋아요 수 집계

### 2.3 보조/외부 시스템
- **LDAP 인증 시스템**: 어드민 식별
- **이벤트 시스템**: 브랜드 비활성화 이벤트 → 상품 일괄 비활성화 처리

---

## 3. 유저 시나리오 기반 기능 정의

### 3.1 어드민 사용자 시나리오

#### 시나리오 1: 브랜드 관리
```
AS AN 어드민 사용자
I WANT TO 브랜드를 등록/수정/조회/비활성화할 수 있다
SO THAT 판매 가능한 브랜드 카탈로그를 관리할 수 있다

Given: LDAP 인증된 어드민 사용자가 로그인한 상태
When: 브랜드 등록/수정 요청 시
Then: 브랜드 정보가 저장되고, 어드민에게 결과가 반환된다

Given: 활성화된 브랜드에 활성 상품이 10개 있는 상태
When: 브랜드 비활성화 요청 시
Then: 
  - 브랜드 상태가 INACTIVE로 변경된다
  - BrandDeactivatedEvent가 발행된다
  - 이벤트 리스너가 해당 브랜드의 모든 상품을 INACTIVE로 일괄 업데이트한다
  - 각 상품의 변경 이력이 ProductHistory에 스냅샷으로 저장된다
```

**기능 요구사항**:
- `POST /api-admin/v1/brands` - 브랜드 등록
- `PUT /api-admin/v1/brands/{brandId}` - 브랜드 정보 수정
- `GET /api-admin/v1/brands?page=0&size=20&status=ACTIVE&sort=createdAt,desc` - 브랜드 목록 조회 (페이징, 필터링, 정렬)
- `GET /api-admin/v1/brands/{brandId}` - 브랜드 상세 조회
- `DELETE /api-admin/v1/brands/{brandId}` - 브랜드 비활성화 (상태 변경)

**비기능 요구사항**:
- LDAP 헤더 필수 검증 (`X-Loopers-Ldap: loopers.admin`)
- 브랜드 비활성화 시 트랜잭션 분리 (브랜드 상태 변경 → 이벤트 → 비동기 상품 처리)
- 상품 수가 많아도 응답 시간 2초 이내 (브랜드 상태만 먼저 변경 후 응답)

---

#### 시나리오 2: 상품 관리
```
AS AN 어드민 사용자
I WANT TO 상품을 등록/수정/조회/비활성화할 수 있다
SO THAT 판매 가능한 상품 카탈로그를 관리할 수 있다

Given: 활성화된 브랜드 A가 존재하는 상태
When: 브랜드 A에 속한 상품을 등록 요청 시
Then: 
  - 브랜드 A의 존재 여부를 확인한다
  - 상품이 저장되고, 초기 스냅샷이 ProductHistory에 저장된다

Given: 상품 P가 브랜드 A에 속한 상태
When: 상품 P의 브랜드를 B로 변경 요청 시
Then:
  - 브랜드 B의 존재 여부를 확인한다
  - 상품 P의 브랜드가 B로 변경된다
  - 변경 시점의 전체 상품 정보가 ProductHistory에 스냅샷으로 저장된다

Given: 재고가 0인 상품이 있는 상태
When: 재고 소진 처리 요청 시
Then:
  - 상품 상태가 OUT_OF_STOCK으로 변경된다
  - 고객 API에서 해당 상품은 "품절" 표시와 함께 조회된다
```

**기능 요구사항**:
- `POST /api-admin/v1/products` - 상품 등록
    - 브랜드 존재 여부 검증
    - 초기 스냅샷 자동 저장
- `PUT /api-admin/v1/products/{productId}` - 상품 정보 수정
    - 브랜드 변경 가능
    - 변경 시점 스냅샷 자동 저장
- `GET /api-admin/v1/products?page=0&size=20&brandId={brandId}&status=ACTIVE&sort=name,asc` - 상품 목록 조회
- `GET /api-admin/v1/products/{productId}` - 상품 상세 조회
- `DELETE /api-admin/v1/products/{productId}` - 상품 비활성화
- `GET /api-admin/v1/products/{productId}/history` - 상품 변경 이력 조회

**비기능 요구사항**:
- 상품 등록/수정 시 브랜드 검증은 Application Layer에서 수행
- 스냅샷 저장은 동일 트랜잭션 내에서 처리
- 이력 조회는 페이징 지원 (변경 시점 역순 정렬)

---

### 3.2 고객 사용자 시나리오

#### 시나리오 3: 브랜드/상품 조회
```
AS A 고객
I WANT TO 활성화된 브랜드와 상품을 조회할 수 있다
SO THAT 구매할 상품을 찾을 수 있다

Given: 브랜드 목록에 ACTIVE 브랜드 5개, INACTIVE 브랜드 3개가 있는 상태
When: 고객이 브랜드 목록 조회 시
Then: ACTIVE 상태인 5개 브랜드만 반환된다

Given: 상품 목록에 ACTIVE 상품 10개, OUT_OF_STOCK 상품 2개, INACTIVE 상품 3개가 있는 상태
When: 고객이 상품 목록 조회 시
Then: 
  - ACTIVE 상품 10개는 "구매 가능"으로 표시
  - OUT_OF_STOCK 상품 2개는 "품절"로 표시
  - INACTIVE 상품 3개는 반환되지 않음
```

**기능 요구사항**:
- `GET /api/v1/brands?page=0&size=20` - 활성 브랜드 목록 조회
- `GET /api/v1/brands/{brandId}` - 브랜드 상세 조회 (ACTIVE만)
- `GET /api/v1/products?page=0&size=20&brandId={brandId}` - 활성/품절 상품 목록 조회
- `GET /api/v1/products/{productId}` - 상품 상세 조회 (ACTIVE, OUT_OF_STOCK만)

**비기능 요구사항**:
- LDAP 인증 불필요
- 조회 성능 최적화 (인덱스: status, brand_id)
- 품절 상품도 노출하되, 구매 불가 표시

---

#### 시나리오 4: 상품 좋아요
```
AS A 고객
I WANT TO 관심 있는 상품에 좋아요를 누를 수 있다
SO THAT 나중에 다시 찾아볼 수 있다

Given: 고객 C가 로그인한 상태
When: 상품 P에 좋아요 클릭 시
Then:
  - ProductLike 레코드가 생성된다
  - 상품 P의 좋아요 수가 1 증가한다

Given: 고객 C가 상품 P에 이미 좋아요를 누른 상태
When: 좋아요 다시 클릭 시
Then:
  - ProductLike 레코드가 삭제된다
  - 상품 P의 좋아요 수가 1 감소한다
```

**기능 요구사항**:
- `POST /api/v1/products/{productId}/likes` - 좋아요 추가
- `DELETE /api/v1/products/{productId}/likes` - 좋아요 취소
- `GET /api/v1/products/{productId}` 응답에 좋아요 수 포함

**비기능 요구사항**:
- 동일 사용자의 중복 좋아요 방지 (Unique 제약)
- 좋아요 수는 집계 테이블 또는 캐시 활용 (성능)

---

## 4. 상태 전이도

### 4.1 브랜드 상태
```
[등록 시] → PENDING
           ↓ (승인)
         ACTIVE ←→ SCHEDULED (예약 판매)
           ↓ (비활성화)
        INACTIVE
```

### 4.2 상품 상태
```
[등록 시] → PENDING
           ↓ (승인)
         ACTIVE ←→ SCHEDULED (예약 판매)
           ↓ (재고 소진)      ↓ (비활성화)
      OUT_OF_STOCK        INACTIVE
           ↓ (재입고)
         ACTIVE
```

**상태별 의미**:
- `PENDING`: 등록 완료, 승인 대기 (향후 확장)
- `ACTIVE`: 활성 상태, 고객에게 노출
- `INACTIVE`: 비활성 상태, 고객에게 미노출
- `SCHEDULED`: 예약 판매 (특정 시점부터 활성화, 향후 확장)
- `OUT_OF_STOCK`: 품절 (고객에게 노출되지만 구매 불가)

---

## 5. 데이터 일관성 정책

### 5.1 브랜드-상품 연쇄 비활성화
- **트리거**: 브랜드 상태가 ACTIVE → INACTIVE 변경
- **처리 방식**:
    1. 브랜드 상태 변경 (트랜잭션 A)
    2. `BrandDeactivatedEvent` 발행
    3. 이벤트 리스너가 비동기로 상품 일괄 업데이트 (트랜잭션 B)
       ```sql
       UPDATE products 
       SET status = 'INACTIVE', updated_at = NOW() 
       WHERE brand_id = ? AND status = 'ACTIVE'
       ```
    4. 각 상품의 스냅샷을 ProductHistory에 저장

- **일시적 불일치 허용**:
    - 브랜드는 즉시 비활성화되지만, 상품은 수 초 뒤 비활성화
    - 고객 조회 시 브랜드가 INACTIVE면 해당 상품도 필터링

### 5.2 상품 변경 이력 스냅샷
- **저장 시점**: 상품 등록/수정 시
- **저장 내용**: 상품의 모든 필드 + 버전 정보
- **구조**:
  ```
  ProductHistory {
    id: Long
    productId: Long (FK)
    version: Integer
    brandId: Long
    name: String
    price: BigDecimal
    status: String
    stockQuantity: Integer
    ... (모든 필드)
    changedAt: LocalDateTime
    changedBy: String
  }
  ```

### 5.3 재고 관리
- **재고 소진 조건**: `stockQuantity <= 0`
- **자동 상태 변경**: 재고가 0이 되면 상태를 OUT_OF_STOCK으로 변경
- **재입고 처리**: 재고가 다시 증가하면 ACTIVE로 복원

---

## 6. API 모듈 분리 전략

### 6.1 모듈 구조
```
project-root/
├── admin-api/           # 어드민 API 모듈
│   ├── controller/      # Admin*Controller
│   ├── dto/             # Admin*Request, Admin*Response
│   └── service/         # AdminBrandService, AdminProductService
├── customer-api/        # 고객 API 모듈
│   ├── controller/      # Customer*Controller
│   ├── dto/             # *Response (조회용)
│   └── service/         # CustomerBrandService, CustomerProductService
├── domain/              # 공통 도메인 모듈
│   ├── model/           # Brand, Product, ProductHistory, ProductLike
│   ├── repository/      # *Repository 인터페이스
│   └── event/           # BrandDeactivatedEvent
└── infrastructure/      # 공통 인프라 모듈
    ├── persistence/     # JPA 구현
    └── config/          # DB, Event 설정
```

### 6.2 DTO 분리 전략
- **어드민 API**:
    - `CreateBrandRequest`, `UpdateBrandRequest`
    - `BrandAdminResponse` (등록일, 수정일, 상태, 등록자 포함)
    - `ProductAdminResponse` (원가, 재고, 상태, 이력 링크 포함)

- **고객 API**:
    - `BrandResponse` (브랜드명, 로고, 설명만)
    - `ProductResponse` (상품명, 가격, 이미지, 좋아요 수, 재고 상태)

### 6.3 책임 분리
- **AdminBrandService**: 브랜드 등록/수정/비활성화, 이벤트 발행
- **AdminProductService**: 상품 CRUD, 스냅샷 저장, 브랜드 검증
- **CustomerBrandService**: ACTIVE 브랜드만 조회
- **CustomerProductService**: ACTIVE/OUT_OF_STOCK 상품만 조회, 좋아요 처리

---

## 7. 비기능 요구사항

### 7.1 성능
- 브랜드 목록 조회: 500ms 이내
- 상품 목록 조회: 1초 이내
- 브랜드 비활성화: 2초 이내 (응답 기준, 상품 처리는 비동기)
- 좋아요 처리: 300ms 이내

### 7.2 확장성
- 페이징: Pageable 인터페이스 사용
- 필터링/정렬: Specification 패턴 또는 QueryDSL 고려
- 검색: 나중에 Elasticsearch 연동 가능하도록 Repository 인터페이스 추상화

### 7.3 보안
- 어드민 API: LDAP 헤더 필수 검증
- 고객 API: 좋아요 기능은 인증 필요, 조회는 비인증 허용

### 7.4 데이터 정합성
- 브랜드-상품 참조 무결성: FK 제약 + Application Layer 검증
- 상품 변경 이력: 트랜잭션 내 스냅샷 저장 보장
- 재고-상태 동기화: 재고 변경 시 상태 자동 업데이트

---

## 8. 잠재 리스크 및 고려사항

### 8.1 브랜드 비활성화 시 대량 상품 처리
- **문제**: 브랜드 하나에 상품 10,000개가 있을 때 일괄 업데이트 시 DB 락 증가
- **완화 방안**:
    - 배치 사이즈 제한 (1000개씩 분할 처리)
    - 실패 시 재시도 메커니즘
    - 어드민에게 진행 상태 노출 (옵션)

### 8.2 스냅샷 테이블 증가
- **문제**: 상품이 자주 수정되면 ProductHistory 테이블 크기 급증
- **완화 방안**:
    - 파티셔닝 (월별, 연도별)
    - 오래된 이력 아카이빙 정책 (예: 2년 이상 된 이력은 별도 저장소)

### 8.3 일시적 데이터 불일치
- **문제**: 브랜드 비활성화 후 상품 비활성화까지 수 초 간 불일치
- **완화 방안**:
    - 고객 조회 시 브랜드 상태로 필터링 (브랜드가 INACTIVE면 상품도 제외)
    - 모니터링: 이벤트 처리 실패 시 알림

### 8.4 좋아요 동시성
- **문제**: 동일 상품에 동시 좋아요 시 카운트 불일치
- **완화 방안**:
    - Unique 제약으로 중복 방지
    - 좋아요 수는 집계 쿼리로 실시간 계산 또는 캐시 활용

---

## 9. 다음 단계

1. **시퀀스 다이어그램**: 브랜드 비활성화 시 상품 연쇄 비활성화 흐름
2. **클래스 다이어그램**: Brand, Product, ProductHistory, ProductLike 도메인 책임
3. **ERD**: 테이블 구조, 관계, 인덱스 설계

---

## 10. 용어 정리

| 용어 | 설명 |
|------|------|
| LDAP | Lightweight Directory Access Protocol, 사내 사용자 인증 |
| 스냅샷 | 특정 시점의 데이터 전체 상태 복사본 |
| 연쇄 비활성화 | 브랜드 비활성화 시 모든 상품도 함께 비활성화 |
| Specification 패턴 | 동적 쿼리 조건 조합을 위한 디자인 패턴 |
| Pageable | Spring Data의 페이징/정렬 추상화 인터페이스 |