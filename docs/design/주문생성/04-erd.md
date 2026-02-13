6️⃣ERD                                                                                                                                   │
│                                                                                                                                          │
│ 왜 필요한가                                                                                                                              │
│                                                                                                                                          │
│ FK 없이 ID 참조만 사용하므로, 논리적 관계를 문서로 명확히 해야 한다. VO는 DB 컬럼으로 풀어져 저장된다.                                   │
│                                                                                                                                          │
│ 검증 포인트                                                                                                                              │
│                                                                                                                                          │
│ - stock.product_id에 UNIQUE 제약 (1:1)                                                                                                   │
│ - order_item의 스냅샷 컬럼이 실제로 어떻게 매핑되는지                                                                                    │
│ - orders 테이블명 (MySQL 예약어 회피)                                                                                                    │
│ - VO(Money, Quantity)는 BIGINT 컬럼으로, ProductSnapshot은 개별 컬럼으로 매핑                                                            │
│                                                                                                                                          │
│ erDiagram                                                                                                                                │
│     brand {                                                                                                                              │
│         BIGINT id PK "AUTO_INCREMENT"                                                                                                    │
│         VARCHAR name "NOT NULL"                                                                                                          │
│         DATETIME created_at "NOT NULL"                                                                                                   │
│         DATETIME updated_at "NOT NULL"                                                                                                   │
│         DATETIME deleted_at "NULL"                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     product {                                                                                                                            │
│         BIGINT id PK "AUTO_INCREMENT"                                                                                                    │
│         BIGINT brand_id "NOT NULL"                                                                                                       │
│         VARCHAR name "NOT NULL"                                                                                                          │
│         BIGINT price "NOT NULL (Money VO)"                                                                                               │
│         VARCHAR description "NULL"                                                                                                       │
│         DATETIME created_at "NOT NULL"                                                                                                   │
│         DATETIME updated_at "NOT NULL"                                                                                                   │
│         DATETIME deleted_at "NULL"                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     stock {                                                                                                                              │
│         BIGINT id PK "AUTO_INCREMENT"                                                                                                    │
│         BIGINT product_id "NOT NULL, UNIQUE"                                                                                             │
│         BIGINT quantity "NOT NULL, DEFAULT 0"                                                                                            │
│         DATETIME created_at "NOT NULL"                                                                                                   │
│         DATETIME updated_at "NOT NULL"                                                                                                   │
│         DATETIME deleted_at "NULL"                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     orders {                                                                                                                             │
│         BIGINT id PK "AUTO_INCREMENT"                                                                                                    │
│         BIGINT member_id "NOT NULL"                                                                                                      │
│         VARCHAR status "NOT NULL"                                                                                                        │
│         BIGINT total_amount "NOT NULL (Money VO)"                                                                                        │
│         DATETIME created_at "NOT NULL"                                                                                                   │
│         DATETIME updated_at "NOT NULL"                                                                                                   │
│         DATETIME deleted_at "NULL"                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     order_item {                                                                                                                         │
│         BIGINT id PK "AUTO_INCREMENT"                                                                                                    │
│         BIGINT order_id "NOT NULL"                                                                                                       │
│         BIGINT product_id "NOT NULL"                                                                                                     │
│         VARCHAR status "NOT NULL"                                                                                                        │
│         VARCHAR product_name "NOT NULL (ProductSnapshot)"                                                                                │
│         BIGINT product_price "NOT NULL (ProductSnapshot.Money)"                                                                          │
│         VARCHAR brand_name "NOT NULL (ProductSnapshot)"                                                                                  │
│         BIGINT quantity "NOT NULL (Quantity VO)"                                                                                         │
│         DATETIME created_at "NOT NULL"                                                                                                   │
│         DATETIME updated_at "NOT NULL"                                                                                                   │
│         DATETIME deleted_at "NULL"                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     brand ||--o{ product : "has"                                                                                                         │
│     product ||--|| stock : "has"                                                                                                         │
│     member ||--o{ orders : "places"                                                                                                      │
│     orders ||--o{ order_item : "contains"                                                                                                │
│     product ||--o{ order_item : "referenced by"                                                                                          │
│                                                                                                                                          │
│ 읽는 법                                                                                                                                  │
│                                                                                                                                          │
│ - VO → 컬럼 매핑: Money VO는 BIGINT 단일 컬럼, Quantity VO도 BIGINT 단일 컬럼, ProductSnapshot은 3개 컬럼(product_name, product_price,   │
│ brand_name)으로 풀어진다.                                                                                                                │
│ - stock ↔ product 1:1: 하나의 상품에 하나의 재고 레코드. UNIQUE 제약으로 보장.                                                           │
│ - 논리적 참조만 존재: FK 없이 애플리케이션 레벨에서 ID 참조.       
️⃣잠재 리스크                                                                                                                           │
│                                                                                                                                          │
│ 트랜잭션 범위                                                                                                                            │
│                                                                                                                                          │
│ Stock 락 + Order/OrderItem 저장이 단일 트랜잭션이므로, 주문 항목 수가 많으면 락 보유 시간이 길어진다.                                    │
│ - (A) 현재 유지 → 일반적 커머스 수준에서 충분. 단순하고 All-or-Nothing 보장 명확.                                                        │
│ - (B) 2-phase (예약→확정) → 트랜잭션 분리 가능하나 복잡도 증가. 현 시점에서는 오버엔지니어링.                                            │
│                                                                                                                                          │
│ 비관적 락 병목                                                                                                                           │
│                                                                                                                                          │
│ 인기 상품에 동시 주문이 몰리면 Stock row 락 대기 발생.                                                                                   │
│ - (A) 비관적 락 유지 → 정합성 확실, 트래픽이 극단적이지 않으면 충분.                                                                     │
│ - (B) Redis 분산 락 → 수평 확장 시 고려. 단일 DB에서는 불필요.                                                                           │
│                                                                                                                                          │
│ 데드락 방지                                                                                                                              │
│                                                                                                                                          │
│ 설계에 이미 반영: productId 오름차순으로 정렬 후 개별 락 획득 (StockDeductionService 책임). 애플리케이션 레벨에서 순서를 보장하는 것이   │
│ DB 엔진 의존성보다 안전.                                                                                                                 │
│                                                                                                                                          │
│ VO JPA 매핑 복잡도                                                                                                                       │
│                                                                                                                                          │
│ @Embeddable VO 사용 시 JPA 매핑이 추가된다. 특히 ProductSnapshot 내부의 Money VO는 중첩 @Embeddable이 된다.                              │
│ - @AttributeOverrides로 컬럼명을 명시적으로 지정하여 해결.                                                                               │
│ - 복잡하다면 ProductSnapshot.price만 Long으로 직접 저장하는 것도 선택지.                                                                 │
│                                                                                                                                          │
│ 스냅샷 시점 차이                                                                                                                         │
│                                                                                                                                          │
│ Facade에서 Product/Brand를 조회한 시점과 OrderService에서 저장하는 시점의 미세한 차이가 있으나, 동일 HTTP 요청 내이므로 비즈니스적으로   │
│ 수용 가능.                                                                                                                               │
│                                                                                                                                          │
│ ---                                                                                                                                      │
│ 생성할 파일 목록                                                                                                                         │
│                                                                                                                                          │
│ domain 레이어 (com.loopers.domain)                                                                                                       │
│                                                                                                                                          │
│ 공통 VO                                                                                                                                  │
│ - domain/vo/Money.java — 가격/금액 VO (@Embeddable, value >= 0)                                                                          │
│ - domain/vo/Quantity.java — 수량 VO (@Embeddable, value > 0)                                                                             │
│                                                                                                                                          │
│ Brand                                                                                                                                    │
│ - domain/brand/BrandModel.java                                                                                                           │
│ - domain/brand/BrandRepository.java                                                                                                      │
│                                                                                                                                          │
│ Product                                                                                                                                  │
│ - domain/product/ProductModel.java — Money VO 사용                                                                                       │
│ - domain/product/ProductRepository.java                                                                                                  │
│ - domain/product/ProductService.java                                                                                                     │
│                                                                                                                                          │
│ Stock                                                                                                                                    │
│ - domain/stock/StockModel.java — deduct(Quantity), hasEnoughStock(Quantity) 메서드 포함                                                  │
│ - domain/stock/StockRepository.java                                                                                                      │
│ - domain/stock/StockDeductionService.java — 도메인 서비스: All-or-Nothing 재고 차감                                                      │
│                                                                                                                                          │
│ Order                                                                                                                                    │
│ - domain/order/OrderModel.java — Money VO 사용                                                                                           │
│ - domain/order/OrderStatus.java                                                                                                          │
│ - domain/order/OrderItemModel.java — ProductSnapshot, Quantity VO 사용                                                                   │
│ - domain/order/OrderItemStatus.java                                                                                                      │
│ - domain/order/ProductSnapshot.java — @Embeddable VO (productName, Money price, brandName)                                               │
│ - domain/order/OrderRepository.java                                                                                                      │
│ - domain/order/OrderItemRepository.java                                                                                                  │
│ - domain/order/OrderService.java                                                                                                         │
│                                                                                                                                          │
│ infrastructure 레이어 (com.loopers.infrastructure)                                                                                       │
│                                                                                                                                          │
│ - infrastructure/brand/BrandJpaRepository.java                                                                                           │
│ - infrastructure/brand/BrandRepositoryImpl.java                                                                                          │
│ - infrastructure/product/ProductJpaRepository.java                                                                                       │
│ - infrastructure/product/ProductRepositoryImpl.java                                                                                      │
│ - infrastructure/stock/StockJpaRepository.java — @Lock(PESSIMISTIC_WRITE) 포함                                                           │
│ - infrastructure/stock/StockRepositoryImpl.java                                                                                          │
│ - infrastructure/order/OrderJpaRepository.java                                                                                           │
│ - infrastructure/order/OrderRepositoryImpl.java                                                                                          │
│ - infrastructure/order/OrderItemJpaRepository.java                                                                                       │
│ - infrastructure/order/OrderItemRepositoryImpl.java                                                                                      │
│                                                                                                                                          │
│ application 레이어 (com.loopers.application)                                                                                             │
│                                                                                                                                          │
│ - application/order/OrderFacade.java                                                                                                     │
│ - application/order/OrderInfo.java                                                                                                       │
│                                                                                                                                          │
│ interfaces 레이어 (com.loopers.interfaces.api)                                                                                           │
│                                                                                                                                          │
│ - interfaces/api/order/OrderV1Controller.java                                                                                            │
│ - interfaces/api/order/OrderV1ApiSpec.java                                                                                               │
│ - interfaces/api/order/OrderV1Dto.java                                                                                                   │
│                                                                                                                                          │
│ 참조할 기존 패턴 파일                                                                                                                    │
│                                                                                                                                          │
│ - domain/example/ExampleModel.java — 엔티티 패턴 (BaseEntity 상속, guard())                                                              │
│ - domain/example/ExampleService.java — 서비스 패턴 (@Component + @Transactional)                                                         │
│ - application/example/ExampleFacade.java — Facade 패턴                                                                                   │
│ - interfaces/api/member/MemberV1Controller.java — 헤더 인증 + ApiResponse 패턴                                                           │
│ - modules/jpa/src/main/java/com/loopers/domain/BaseEntity.java — BaseEntity                                                              │
│                                                                                                                                          │
│ 검증 방법                                                                                                                                │
│                                                                                                                                          │
│ 1. 단위 테스트: Money/Quantity VO 불변식, StockModel.deduct(), ProductSnapshot 생성, OrderModel 생성                                     │
│ 2. 통합 테스트: OrderService.createOrder() — 성공/재고부족 롤백, StockDeductionService 동시성 시나리오                                   │
│ 3. E2E 테스트: POST /api/v1/orders 호출 → 주문 생성 확인, 재고 차감 확인                                                                 │
│ 4. HTTP 파일: http/commerce-api/order-v1.http로 수동 확인  