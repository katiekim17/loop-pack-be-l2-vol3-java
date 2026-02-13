# 감성 이커머스 시스템 요구사항 명세서 (v1)

## 1️⃣ 도메인 구조

### v1 핵심 도메인
| 도메인     | 책임 | 주요 엔티티 |
|---------|------|------------|
| **브랜드** | 브랜드 정보 관리 | Brand |
| **상품**  | 상품 카탈로그 관리 | Product, ProductOption, ProductImage |
| **좋아요** | 사용자 관심 상품 관리 | Like |
| **주문**  | 주문 생성 및 재고 예약 관리 | Order, OrderItem, Stock |

### 도메인 간 관계
```
Brand (브랜드)
  └─→ Product (상품) : 1:N [brand_id로 참조]

Product (상품)
  ├─→ ProductOption (상품 옵션) : 1:N [product_id로 참조]
  ├─→ ProductImage (상품 이미지) : 1:N [product_id로 참조]
  └─→ Like (좋아요) : 1:N [product_id로 참조]

ProductOption (상품 옵션)
  └─→ Stock (재고) : 1:1 [product_option_id로 참조]

User (사용자)
  ├─→ Like (좋아요) : 1:N [user_id로 참조]
  └─→ Order (주문) : 1:N [user_id로 참조]

Order (주문)
  └─→ OrderItem (주문 항목) : 1:N [order_id로 참조]

```

### 핵심 개념
- **Brand**: 상품을 제공하는 브랜드
- **Product**: 브랜드가 판매하는 상품의 기본 정보 및 판매 상태 관리
    - 상태 기반 관리 (DRAFT / ACTIVE / INACTIVE)
    - 삭제 불가
    - ACTIVE는 운영자 수동 전이
- **ProductOption**: 상품의 판매 단위
    - 상태 보유 (ON_SALE / SOLD_OUT / STOPPED)
    - 재고는 옵션 단위
    - 삭제 불가
    - 구매 조건:
        - Product ACTIVE
        - Option ON_SALE
        - stock > 0
- **Stock**: 옵션 단위 재고 관리
    - stock_quantity : 실제 보유 재고
    - reserved_quantity : 잠가 둔 재고
    - available : 현재 주문 가능한 수량
- **ProductImage**: 상품의 이미지들 (여러 장 가능, product_id 보유)
- **Like**: 사용자의 상품 좋아요
    - Unique(user_id, product_id)
    - Product ACTIVE일 때만 등록 가능
    - 취소는 멱등
    - Product INACTIVE 시 기존 Like는 유지
- **Order**: 사용자의 주문
    - 상태 기반 관리 (CREATED / CONFIRMED / CANCELLED)
    - 예약 재고 기반 처리
    - All-or-Nothing 정책 적용
      **OrderItem**: 주문 시점의 상품 정보 스냅샷 보존
    - productName
    - brandName
    - optionName
    - optionAttributes
    - thumbnailImageUrl
    - orderPrice (Money)
    - quantity (Quantity)


### 상품 및 주문 시스템 설계 원칙
**1. 데이터 연결 및 무결성 (No Foreign Key)**
- **ID 기반 참조**: 데이터베이스 강제 연결(FK) 대신, 서비스 로직에서 상품과 옵션을 연결합니다.
- **유연한 관리**: 시스템이 멈추지 않고 유연하게 데이터를 처리하며, 데이터 간의 정합성은 애플리케이션 서비스 단계에서 꼼꼼히 체크합니다.

**2. 상태 중심 운영 (State Control)**
- **상태로 판매 통제**: 상품의 판매 가능 여부는 '판매중', '품절', '중지' 등 상태값으로 관리합니다.
- **수동 제어 우선**: 시스템 자동 처리보다는 운영자가 직접 상황에 맞춰 판매를 제어할 수 있도록 설계하여 운영의 묘를 살립니다.

**3. 데이터 보존 (Soft Delete)**
- **삭제 금지**: 브랜드, 상품, 옵션 데이터는 절대 삭제하지 않습니다.
- **히스토리 유지**: 상품이 품절되거나 사라져도 과거의 주문 내역이나 고객의 '좋아요' 기록은 그대로 보존하여 데이터 추적성을 확보합니다.

**4. 데이터 일관성 전략 (Consistency)**
- **재고와 주문 (절대 일관성)**: 결제와 재고 차감은 **'모두 성공하거나 모두 실패'**해야 합니다. 수량 오차를 0으로 유지합니다.
- **좋아요 수 (점진적 반영)**: 수치는 실시간으로 아주 약간의 오차가 있을 수 있으나, 시스템 부하를 줄이기 위해 비동기 방식으로 빠르게 업데이트합니다.

### 핵심 설계 의도
- **옵션 중심 관리**: 같은 상품이라도 옵션(사이즈, 색상 등)에 따라 가격과 재고가 다를 수 있음
- **판매의 실질 단위**: 고객이 구경하는 것은 '상품'이지만, 실제로 장바구니에 담고 **결제하는 단위는 '옵션'**
- 좋아요는 **상품 단위**로 관리 (옵션 단위 아님)
- **FK 제약 없음**: 애플리케이션 레벨에서 참조 무결성 관리
