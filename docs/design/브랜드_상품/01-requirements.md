# 감성 이커머스 시스템 요구사항 명세서 (v1)

## 1️⃣ 도메인 구조

### v1 핵심 도메인
| 도메인 | 책임 | 주요 엔티티 |
|--------|------|------------|
| **브랜드** | 브랜드 정보 관리 | Brand |
| **상품** | 상품 카탈로그 관리 | Product, ProductOption, ProductImage |
| **좋아요** | 사용자 관심 상품 관리 | Like |

### 도메인 간 관계
```
Brand (브랜드)
  └─→ Product (상품) : 1:N [brand_id로 참조]

Product (상품)
  ├─→ ProductOption (상품 옵션) : 1:N [product_id로 참조]
  ├─→ ProductImage (상품 이미지) : 1:N [product_id로 참조]
  └─→ Like (좋아요) : 1:N [product_id로 참조]

User (사용자)
  └─→ Like (좋아요) : 1:N [user_id로 참조]
```

**핵심 개념:**
- **Brand**: 상품을 제공하는 브랜드
- **Product**: 브랜드가 판매하는 상품 (기본 정보, brand_id 보유)
- **ProductOption**: 상품의 판매 단위 (사이즈, 색상 등 + 가격 + 재고, product_id 보유)
- **ProductImage**: 상품의 이미지들 (여러 장 가능, product_id 보유)
- **Like**: 사용자의 상품 좋아요 (v1에서는 인증 없이 임시 식별자 사용, product_id 보유)

**설계 의도:**
- 상품과 옵션을 분리하여 **옵션별로 가격과 재고를 독립적으로 관리**
- 같은 상품이라도 옵션(사이즈, 색상 등)에 따라 가격과 재고가 다를 수 있음
- 좋아요는 **상품 단위**로 관리 (옵션 단위 아님)
- 향후 장바구니, 주문 기능 추가 시 **옵션이 판매 단위**가 됨
- **FK 제약 없음**: 애플리케이션 레벨에서 참조 무결성 관리

---

## 2️⃣ 유저 시나리오 기반 기능 정의

### US-1. 브랜드 탐색
**시나리오:**  
사용자는 여러 브랜드를 둘러보며 관심 있는 브랜드를 찾는다.

**주요 흐름:**
1. 브랜드 목록 페이지 접속
2. 브랜드 카드(썸네일, 이름) 확인
3. 특정 브랜드 클릭 → 브랜드 상세 정보 조회

**제공 기능:**
- 브랜드 정보 조회

---

### US-2. 상품 탐색
**시나리오:**  
사용자는 브랜드의 상품 목록을 둘러보고, 정렬 기능을 활용하여 원하는 상품을 찾는다. 관심 있는 상품의 상세 정보를 확인한다.

**주요 흐름:**
1. 전체 상품 목록 또는 특정 브랜드의 상품 목록 조회
2. 정렬 옵션 선택 (최신순, 가격순, 인기순)
3. 상품 카드(이미지, 이름, 최저가, 좋아요 수) 확인
4. 특정 상품 클릭 → 상품 상세 정보 조회
5. 상품 옵션별 가격, 재고 확인

**제공 기능:**
- 상품 목록 조회 (브랜드 필터링, 정렬, 페이지네이션)
- 상품 상세 정보 조회

---

### US-3. 상품 좋아요
**시나리오:**  
사용자는 마음에 드는 상품에 좋아요를 누르고, 내가 좋아요한 상품 목록을 확인한다.

**주요 흐름:**
1. 상품 목록 또는 상세 페이지에서 좋아요 버튼 클릭
2. 좋아요 등록/취소 토글
3. 좋아요 수 실시간 업데이트 (비동기)
4. 내 좋아요 목록 페이지에서 좋아요한 상품들 확인

**제공 기능:**
- 상품 좋아요 등록
- 상품 좋아요 취소
- 내 좋아요 목록 조회

**참고:**
- 좋아요 기능의 상세 요구사항(동기/비동기 처리, 카운트 업데이트 정책 등)은 별도 문서 참조

---

## 3️⃣ 기능별 상세 요구사항

### 🔹 브랜드 (Brand)

#### FR-B-01. 브랜드 정보 조회
**API:** `GET /api/v1/brands/{brandId}`  
**인증:** 불필요

**Path Parameter:**
- `brandId` (Long): 브랜드 ID

**반환 정보:**
```json
{
  "brandId": 1,
  "name": "브랜드명",
  "description": "브랜드 설명",
  "logoImageUrl": "https://example.com/brand-logo.png",
  "createdAt": "2025-01-01T00:00:00"
}
```

**에러 처리:**
- 존재하지 않는 브랜드 ID → 404 Not Found

---

### 🔹 상품 (Product)

#### FR-P-01. 상품 목록 조회
**API:** `GET /api/v1/products`  
**인증:** 불필요 (로그인 시 좋아요 여부 추가 제공)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `brandId` | Long | X | - | 특정 브랜드의 상품만 필터링 |
| `sort` | String | X | `latest` | 정렬 기준 (`latest`, `price_asc`, `likes_desc`) |
| `page` | Integer | X | 0 | 페이지 번호 |
| `size` | Integer | X | 20 | 페이지당 상품 수 |

**정렬 옵션:**
- `latest` (기본값): 최신 등록순 (createdAt DESC)
- `price_asc`: 최저가 낮은 순 (각 상품의 옵션 중 최저가 기준)
- `likes_desc`: 좋아요 많은 순 (likeCount DESC)

**반환 정보 (비로그인 사용자):**
```json
{
  "content": [
    {
      "productId": 1,
      "name": "상품명",
      "brand": {
        "brandId": 1,
        "name": "브랜드명"
      },
      "thumbnailImageUrl": "https://example.com/product-thumbnail.png",
      "minPrice": 10000,
      "likeCount": 150,
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

**반환 정보 (로그인 사용자):**
```json
{
  "content": [
    {
      "productId": 1,
      "name": "상품명",
      "brand": {
        "brandId": 1,
        "name": "브랜드명"
      },
      "thumbnailImageUrl": "https://example.com/product-thumbnail.png",
      "minPrice": 10000,
      "likeCount": 150,
      "isLikedByMe": true,
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

**비즈니스 규칙:**
- `minPrice`: 해당 상품의 모든 옵션 중 **최저가**를 표시
- `likeCount`: 해당 상품의 좋아요 수 (비동기 업데이트, Eventual Consistency)
- `isLikedByMe`: 로그인한 사용자가 해당 상품을 좋아요 했는지 여부 (로그인 시에만 포함)

**성능 고려사항:**
- 비로그인: 브랜드 정보 Fetch Join, 최저가는 서브쿼리 사용
- 로그인: 추가로 좋아요 여부 확인 (EXISTS 서브쿼리 또는 LEFT JOIN)

---

#### FR-P-02. 상품 상세 정보 조회
**API:** `GET /api/v1/products/{productId}`  
**인증:** 불필요 (로그인 시 좋아요 여부 추가 제공)

**Path Parameter:**
- `productId` (Long): 상품 ID

**반환 정보 (비로그인 사용자):**
```json
{
  "productId": 1,
  "name": "상품명",
  "description": "상품 상세 설명",
  "brand": {
    "brandId": 1,
    "name": "브랜드명"
  },
  "imageUrls": [
    "https://example.com/product-image1.png",
    "https://example.com/product-image2.png"
  ],
  "options": [
    {
      "productOptionId": 1,
      "name": "S 사이즈",
      "price": 10000,
      "stockQuantity": 50,
      "isAvailable": true
    },
    {
      "productOptionId": 2,
      "name": "M 사이즈",
      "price": 10000,
      "stockQuantity": 0,
      "isAvailable": false
    },
    {
      "productOptionId": 3,
      "name": "L 사이즈",
      "price": 12000,
      "stockQuantity": 30,
      "isAvailable": true
    }
  ],
  "likeCount": 150,
  "createdAt": "2025-01-01T00:00:00"
}
```

**반환 정보 (로그인 사용자):**
```json
{
  "productId": 1,
  "name": "상품명",
  "description": "상품 상세 설명",
  "brand": {
    "brandId": 1,
    "name": "브랜드명"
  },
  "imageUrls": [
    "https://example.com/product-image1.png",
    "https://example.com/product-image2.png"
  ],
  "options": [
    {
      "productOptionId": 1,
      "name": "S 사이즈",
      "price": 10000,
      "stockQuantity": 50,
      "isAvailable": true
    }
  ],
  "likeCount": 150,
  "isLikedByMe": true,
  "createdAt": "2025-01-01T00:00:00"
}
```

**비즈니스 규칙:**
- **옵션별 가격**: 각 옵션은 독립적인 가격을 가짐
- **재고 표시**: `stockQuantity`로 재고 수량 표시
- **판매 가능 여부**: `isAvailable = stockQuantity > 0`
- 옵션이 없는 상품은 존재하지 않음 (최소 1개 옵션 필수)

**에러 처리:**
- 존재하지 않는 상품 ID → 404 Not Found

---

### 🔹 좋아요 (Like)

> **참고:** 좋아요 기능의 상세 요구사항(동기/비동기 처리, 이벤트 발행, 배치 복구 등)은 별도 문서 참조

#### FR-L-01. 좋아요 등록
**API:** `POST /api/v1/products/{productId}/likes`  
**인증:** 필수 (v1에서는 임시 식별자 사용, v2에서 정식 인증으로 전환)

**처리:**
- 좋아요 등록 (동기)
- 중복 좋아요 방지 (DB Unique 제약)
- 카운트 업데이트 (비동기)

**에러 처리:**
- 이미 좋아요한 상품 → 409 Conflict

---

#### FR-L-02. 좋아요 취소
**API:** `DELETE /api/v1/products/{productId}/likes`  
**인증:** 필수

**처리:**
- 좋아요 삭제 (동기)
- 카운트 업데이트 (비동기)
- 멱등성 보장 (이미 취소된 좋아요 재요청 시 성공 응답)

---

#### FR-L-03. 내 좋아요 목록 조회
**API:** `GET /api/v1/users/me/likes`  
**인증:** 필수

**Query Parameters:**
- `page` (선택, Integer, 기본값: 0): 페이지 번호
- `size` (선택, Integer, 기본값: 20): 페이지 크기

**반환 정보:**
```json
{
  "content": [
    {
      "productId": 1,
      "name": "상품명",
      "brand": {
        "brandId": 1,
        "name": "브랜드명"
      },
      "thumbnailImageUrl": "https://example.com/product-thumbnail.png",
      "minPrice": 10000,
      "likeCount": 150,
      "likedAt": "2025-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 10,
  "totalPages": 1
}
```

---

## 4️⃣ 설계 고려사항

### 확장 포인트

#### 1. 옵션 구조의 유연성
**현재 (v1):**
- 단순 옵션명 + 가격 + 재고
- 예: "S 사이즈", "M 사이즈", "L 사이즈"

**향후 확장 가능성 (v2):**
- 다차원 옵션 지원 (예: 색상 × 사이즈)
- 예: "빨강/S", "빨강/M", "파랑/S", "파랑/M"
- 옵션 그룹 개념 도입 (색상 그룹, 사이즈 그룹)

**설계 시 주의사항:**
- 현재 단순 구조로 시작하되, 다차원 옵션으로 확장 가능하도록 옵션명을 구조화
- 옵션 ID는 불변으로 유지, 옵션 속성 변경 시 새 옵션 생성

---

#### 2. 재고 관리
**현재 (v1):**
- 단순 수량 관리 (`stock_quantity`)
- 재고 조회만 가능

**향후 확장 가능성 (v2):**
- 예약 재고 개념 (주문 생성 시 차감)
- 안전 재고 (품절 임박 알림)
- 재고 히스토리 (입고/출고 이력)

**설계 시 주의사항:**
- 재고 차감은 트랜잭션 내에서 원자적으로 처리
- 동시성 제어 (낙관적 락 또는 비관적 락)

---

#### 3. 가격 정책
**현재 (v1):**
- 옵션별 단일 가격
- 최저가 기준 정렬

**향후 확장 가능성 (v2):**
- 프로모션 가격 (기간 한정 할인)
- 회원 등급별 가격
- 쿠폰 적용 후 가격

**설계 시 주의사항:**
- 가격 이력 관리 (가격 변경 추적)
- 주문 시점 가격 고정 (계약 개념)

---

#### 4. 좋아요 카운트 정합성
**현재 (v1):**
- 좋아요 등록/취소: 동기
- 카운트 업데이트: 비동기 (Eventual Consistency)

**잠재 리스크:**
- 이벤트 발행 실패 시 카운트 불일치
- 대량 좋아요 발생 시 카운트 업데이트 지연

**해결 방안:**
- 배치 작업을 통한 정합성 복구
- 향후 Redis 캐시 도입 (실시간 카운트)

**설계 시 주의사항:**
- 좋아요 수는 "대략적인 인기도" 지표로 사용
- 정확한 수치가 필요한 경우 실시간 COUNT 쿼리 사용

---

### 성능 최적화 포인트

#### 1. 상품 목록 조회
**N+1 문제 방지:**
```
❌ 각 상품마다 브랜드 조회, 최저가 조회, 좋아요 수 조회
✅ Fetch Join + 서브쿼리로 단일 쿼리 구성
```

**쿼리 최적화 전략:**
- 브랜드 정보: Fetch Join
- 최저가: 서브쿼리 (SELECT MIN(price) FROM product_options WHERE ...)
- 좋아요 수: 서브쿼리 (SELECT COUNT(*) FROM likes WHERE ...)
- 좋아요 여부 (로그인 시): EXISTS 서브쿼리 또는 LEFT JOIN

**인덱스 전략:**
- `products(brand_id)`: 브랜드별 상품 필터링
- `products(created_at DESC)`: 최신순 정렬
- `product_options(product_id, price)`: 최저가 계산
- `likes(product_id)`: 좋아요 수 집계
- `likes(user_id, product_id)`: 중복 방지 + 좋아요 여부 확인

---

#### 2. 상품 상세 조회
**Fetch Join 활용:**
```
✅ 상품 + 옵션 + 이미지 + 브랜드를 단일 쿼리로 조회
```

**쿼리 최적화 전략:**
- 옵션 목록: Fetch Join (1:N)
- 이미지 목록: Fetch Join (1:N)
- 브랜드 정보: Fetch Join (N:1)

**주의사항:**
- 1:N 관계가 여러 개면 카테시안 곱 발생 가능
- 필요 시 배치 쿼리로 분리

---

#### 3. 정렬 성능
**최신순 (`latest`):**
- 인덱스: `products(created_at DESC)`
- 추가 계산 없이 인덱스 스캔만으로 정렬 가능

**가격순 (`price_asc`):**
- 각 상품의 최저가 계산 필요
- 서브쿼리 또는 집계 쿼리 사용
- 대용량 데이터 시 성능 이슈 가능 → 캐싱 고려

**인기순 (`likes_desc`):**
- 좋아요 수 집계 필요
- 비동기 업데이트로 인한 지연 허용
- 대용량 데이터 시 Redis 캐시 활용 고려

---

### 잠재 리스크 및 해결 방안

#### 1. 대용량 상품 데이터 처리
**리스크:**
- 상품 수 10만 개 이상 시 목록 조회 성능 저하
- 특히 가격순, 인기순 정렬 시 계산 비용 증가

**해결 방안:**
- 적절한 인덱스 설계
- 페이지네이션 필수 (Offset 방식 → Cursor 방식 고려)
- 자주 조회되는 정렬 결과 캐싱 (Redis)

---

#### 2. 좋아요 동시성 이슈
**리스크:**
- 같은 사용자가 동시에 좋아요 등록 요청
- 여러 사용자가 동시에 같은 상품 좋아요

**해결 방안:**
- DB Unique 제약으로 중복 방지
- 애플리케이션 레벨에서 멱등성 보장
- 낙관적 락 사용 (버전 관리)

---

#### 3. 이미지 로딩 성능
**리스크:**
- 상품 목록에서 이미지 다수 로딩 시 초기 로딩 지연

**해결 방안:**
- CDN 활용
- Lazy Loading (스크롤 시 순차 로딩)
- 썸네일 이미지 최적화 (리사이징, WebP 포맷)
- 이미지 URL은 DB에 저장, 실제 파일은 Object Storage (S3 등)

---

## 5️⃣ 용어 사전

| 용어 | 설명 |
|------|------|
| **브랜드 (Brand)** | 상품을 제공하는 제조사 또는 판매자 |
| **상품 (Product)** | 판매 대상이 되는 아이템의 기본 정보 |
| **상품 옵션 (ProductOption)** | 상품의 실제 판매 단위 (사이즈, 색상 등 구분) |
| **재고 (Stock)** | 판매 가능한 수량 (옵션 단위로 관리) |
| **최저가 (Min Price)** | 상품의 모든 옵션 중 가장 낮은 가격 |
| **좋아요 (Like)** | 사용자가 상품에 대한 관심을 표현하는 행위 (상품 단위) |
| **좋아요 수 (Like Count)** | 해당 상품의 총 좋아요 개수 (인기도 지표) |
| **페이지네이션 (Pagination)** | 대량 데이터를 페이지 단위로 나누어 조회 |
| **Eventual Consistency** | 최종 일관성 - 일시적 불일치를 허용하되 최종적으로 일관성 보장 |

---

**문서 끝**
