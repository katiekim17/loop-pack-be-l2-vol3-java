️⃣시퀀스 다이어그램                                                                                                                     │
│                                                                                                                                          │
│ 왜 필요한가                                                                                                                              │
│                                                                                                                                          │
│ 주문 생성은 Member, Product, Brand, Stock, Order, OrderItem 6개 도메인을 횡단한다. 호출 순서, 트랜잭션 경계, All-or-Nothing 실패 시점을  │
│ 검증해야 한다.                                                                                                                           │
│                                                                                                                                          │
│ 검증 포인트                                                                                                                              │
│                                                                                                                                          │
│ - 트랜잭션 경계가 어디서 시작하고 끝나는지                                                                                               │
│ - StockDeductionService의 책임 범위                                                                                                      │
│ - 비관적 락 획득 순서 (데드락 방지)                                                                                                      │
│ - 재고 부족 시 롤백 시점                                                                                                                 │
│                                                                                                                                          │
│ sequenceDiagram                                                                                                                          │
│     actor User                                                                                                                           │
│     participant Controller as OrderV1Controller                                                                                          │
│     participant Facade as OrderFacade                                                                                                    │
│     participant MemberSvc as MemberService                                                                                               │
│     participant ProductSvc as ProductService                                                                                             │
│     participant OrderSvc as OrderService                                                                                                 │
│     participant StockDeduct as StockDeductionService                                                                                     │
│     participant StockRepo as StockRepository                                                                                             │
│     participant DB as Database                                                                                                           │
│                                                                                                                                          │
│     User->>Controller: POST /api/v1/orders<br/>[Header: X-Loopers-LoginId/LoginPw]<br/>[Body: items[{productId, quantity}]]              │
│     Controller->>Facade: createOrder(loginId, password, request)                                                                         │
│                                                                                                                                          │
│     Note over Facade: 트랜잭션 밖: 인증 + 조회                                                                                           │
│     Facade->>MemberSvc: authenticate(loginId, password)                                                                                  │
│     MemberSvc-->>Facade: MemberModel (memberId)                                                                                          │
│                                                                                                                                          │
│     Facade->>ProductSvc: getProducts(productIds)                                                                                         │
│     ProductSvc-->>Facade: List<ProductModel>                                                                                             │
│     Facade->>ProductSvc: getBrands(brandIds)                                                                                             │
│     ProductSvc-->>Facade: List<BrandModel>                                                                                               │
│                                                                                                                                          │
│     alt 존재하지 않는 상품/브랜드                                                                                                        │
│         Facade-->>Controller: CoreException(NOT_FOUND)                                                                                   │
│         Controller-->>User: 404 Not Found                                                                                                │
│     end                                                                                                                                  │
│                                                                                                                                          │
│     Note over Facade,DB: ── 트랜잭션 시작 (OrderService.createOrder) ──                                                                  │
│                                                                                                                                          │
│     Facade->>OrderSvc: createOrder(memberId, products, brandMap, items)                                                                  │
│                                                                                                                                          │
│     OrderSvc->>StockDeduct: deductAll(items)                                                                                             │
│     Note over StockDeduct: productId 오름차순 정렬 → 데드락 방지                                                                         │
│     loop 각 상품 (productId 오름차순)                                                                                                    │
│         StockDeduct->>StockRepo: findByProductIdWithLock(productId)                                                                      │
│         StockRepo->>DB: SELECT ... FOR UPDATE                                                                                            │
│         DB-->>StockRepo: StockModel (locked)                                                                                             │
│         StockRepo-->>StockDeduct: StockModel                                                                                             │
│                                                                                                                                          │
│         alt 재고 부족                                                                                                                    │
│             Note over StockDeduct,DB: 예외 → 트랜잭션 롤백 (All-or-Nothing)                                                              │
│             StockDeduct-->>OrderSvc: CoreException(BAD_REQUEST)                                                                          │
│             OrderSvc-->>Facade: 예외 전파                                                                                                │
│             Facade-->>Controller: 예외 전파                                                                                              │
│             Controller-->>User: 400 Bad Request                                                                                          │
│         end                                                                                                                              │
│                                                                                                                                          │
│         StockDeduct->>StockDeduct: stock.deduct(Quantity)                                                                                │
│     end                                                                                                                                  │
│     StockDeduct-->>OrderSvc: 차감 완료                                                                                                   │
│                                                                                                                                          │
│     Note over OrderSvc: 주문 생성                                                                                                        │
│     OrderSvc->>OrderSvc: new OrderModel(memberId, Money(totalAmount), CREATED)                                                           │
│     OrderSvc->>DB: INSERT orders                                                                                                         │
│                                                                                                                                          │
│     loop 각 주문 항목                                                                                                                    │
│         OrderSvc->>OrderSvc: new OrderItemModel(orderId, productId,<br/>ProductSnapshot, Quantity, ORDERED)                              │
│     end                                                                                                                                  │
│     OrderSvc->>DB: INSERT order_item × N                                                                                                 │
│                                                                                                                                          │
│     Note over Facade,DB: ── 트랜잭션 커밋 ──                                                                                             │
│                                                                                                                                          │
│     OrderSvc-->>Facade: OrderModel + OrderItems                                                                                          │
│     Facade-->>Controller: OrderInfo                                                                                                      │
│     Controller-->>User: 201 Created + OrderResponse                                                                                      │
│                                                                                                                                          │
│ 읽는 법                                                                                                                                  │
│                                                                                                                                          │
│ - Facade는 조율자: 인증/조회는 트랜잭션 밖에서 처리하여 핵심 쓰기 트랜잭션 범위를 최소화.                                                │
│ - StockDeductionService가 재고 책임: 락 획득 순서, 재고 검증, 차감을 모두 담당. OrderService는 이 결과를 신뢰하고 주문만 생성.           │
│ - 실패 시점이 명확: 재고 부족이면 StockDeductionService에서 즉시 예외 → 전체 롤백.    