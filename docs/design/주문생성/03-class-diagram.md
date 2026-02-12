5️⃣클래스 다이어그램                                                                                                                     │
│                                                                                                                                          │
│ 왜 필요한가                                                                                                                              │
│                                                                                                                                          │
│ 5개 엔티티 + 3개 VO + 도메인 서비스의 책임 분배, 의존 방향을 검증해야 한다.                                                              │
│                                                                                                                                          │
│ 검증 포인트                                                                                                                              │
│                                                                                                                                          │
│ - VO가 어떤 엔티티에서 사용되는지                                                                                                        │
│ - StockDeductionService의 위치와 의존 방향                                                                                               │
│ - OrderService와 StockDeductionService의 책임 경계                                                                                       │
│                                                                                                                                          │
│ classDiagram                                                                                                                             │
│     direction TB                                                                                                                         │
│                                                                                                                                          │
│     class BaseEntity {                                                                                                                   │
│         <<abstract>>                                                                                                                     │
│         #Long id                                                                                                                         │
│         #ZonedDateTime createdAt                                                                                                         │
│         #ZonedDateTime updatedAt                                                                                                         │
│         #ZonedDateTime deletedAt                                                                                                         │
│         +delete()                                                                                                                        │
│         +restore()                                                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class Money {                                                                                                                        │
│         <<VO / Embeddable>>                                                                                                              │
│         -Long value                                                                                                                      │
│         +Money(Long value)                                                                                                               │
│         +getValue() Long                                                                                                                 │
│     }                                                                                                                                    │
│     note for Money "불변식: value >= 0\nProduct.price, Order.totalAmount,\nProductSnapshot.price에 사용"                                 │
│                                                                                                                                          │
│     class Quantity {                                                                                                                     │
│         <<VO / Embeddable>>                                                                                                              │
│         -Long value                                                                                                                      │
│         +Quantity(Long value)                                                                                                            │
│         +getValue() Long                                                                                                                 │
│     }                                                                                                                                    │
│     note for Quantity "불변식: value > 0\nOrderItem.quantity에 사용"                                                                     │
│                                                                                                                                          │
│     class ProductSnapshot {                                                                                                              │
│         <<VO / Embeddable>>                                                                                                              │
│         -String productName                                                                                                              │
│         -Money price                                                                                                                     │
│         -String brandName                                                                                                                │
│         +ProductSnapshot(productName, price, brandName)                                                                                  │
│     }                                                                                                                                    │
│     note for ProductSnapshot "주문 시점 상품 정보 스냅샷\nOrderItem에 @Embedded로 내장"                                                  │
│                                                                                                                                          │
│     class BrandModel {                                                                                                                   │
│         -String name                                                                                                                     │
│         +BrandModel(name)                                                                                                                │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class ProductModel {                                                                                                                 │
│         -Long brandId                                                                                                                    │
│         -String name                                                                                                                     │
│         -Money price                                                                                                                     │
│         -String description                                                                                                              │
│         +ProductModel(brandId, name, price, description)                                                                                 │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class StockModel {                                                                                                                   │
│         -Long productId                                                                                                                  │
│         -Long quantity                                                                                                                   │
│         +StockModel(productId, quantity)                                                                                                 │
│         +deduct(Quantity amount) void                                                                                                    │
│         +hasEnoughStock(Quantity amount) boolean                                                                                         │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class OrderModel {                                                                                                                   │
│         -Long memberId                                                                                                                   │
│         -OrderStatus status                                                                                                              │
│         -Money totalAmount                                                                                                               │
│         +OrderModel(memberId, totalAmount, status)                                                                                       │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class OrderItemModel {                                                                                                               │
│         -Long orderId                                                                                                                    │
│         -Long productId                                                                                                                  │
│         -OrderItemStatus status                                                                                                          │
│         -ProductSnapshot snapshot                                                                                                        │
│         -Quantity quantity                                                                                                               │
│         +OrderItemModel(orderId, productId, status, snapshot, quantity)                                                                  │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class OrderStatus {                                                                                                                  │
│         <<enum>>                                                                                                                         │
│         CREATED                                                                                                                          │
│         CONFIRMED                                                                                                                        │
│         CANCELLED                                                                                                                        │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     class OrderItemStatus {                                                                                                              │
│         <<enum>>                                                                                                                         │
│         ORDERED                                                                                                                          │
│         CANCELLED                                                                                                                        │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     BaseEntity <|-- BrandModel                                                                                                           │
│     BaseEntity <|-- ProductModel                                                                                                         │
│     BaseEntity <|-- StockModel                                                                                                           │
│     BaseEntity <|-- OrderModel                                                                                                           │
│     BaseEntity <|-- OrderItemModel                                                                                                       │
│                                                                                                                                          │
│     ProductModel --> Money : price                                                                                                       │
│     OrderModel --> Money : totalAmount                                                                                                   │
│     OrderModel --> OrderStatus                                                                                                           │
│     OrderItemModel --> ProductSnapshot : snapshot                                                                                        │
│     OrderItemModel --> Quantity : quantity                                                                                               │
│     OrderItemModel --> OrderItemStatus                                                                                                   │
│     ProductSnapshot --> Money : price                                                                                                    │
│                                                                                                                                          │
│     class StockDeductionService {                                                                                                        │
│         <<도메인 서비스>>                                                                                                                │
│         +deductAll(List~OrderItemRequest~ items) void                                                                                    │
│     }                                                                                                                                    │
│     note for StockDeductionService "크로스 엔티티 비즈니스 로직\nproductId 오름차순 락 획득\nAll-or-Nothing 재고 차감"                   │
│                                                                                                                                          │
│     class OrderFacade {                                                                                                                  │
│         +createOrder(loginId, password, request) OrderInfo                                                                               │
│     }                                                                                                                                    │
│     class OrderService {                                                                                                                 │
│         +createOrder(memberId, products, brandMap, items) OrderModel                                                                     │
│     }                                                                                                                                    │
│     class ProductService {                                                                                                               │
│         +getProducts(productIds) List~ProductModel~                                                                                      │
│         +getBrands(brandIds) List~BrandModel~                                                                                            │
│     }                                                                                                                                    │
│                                                                                                                                          │
│     OrderFacade --> MemberService                                                                                                        │
│     OrderFacade --> ProductService                                                                                                       │
│     OrderFacade --> OrderService                                                                                                         │
│     OrderService --> StockDeductionService                                                                                               │
│     OrderService ..> OrderRepository                                                                                                     │
│     OrderService ..> OrderItemRepository                                                                                                 │
│     StockDeductionService ..> StockRepository                                                                                            │
│     ProductService ..> ProductRepository                                                                                                 │
│     ProductService ..> BrandRepository                                                                                                   │
│                                                                                                                                          │
│ 읽는 법                                                                                                                                  │
│                                                                                                                                          │
│ - VO 적용 범위가 명확: Money는 가격/금액이 나오는 3곳, Quantity는 주문 수량, ProductSnapshot은 OrderItem 내 스냅샷. 각각 명확한 불변식을 │
│  가진다.                                                                                                                                 │
│ - StockDeductionService: OrderService에서 분리된 도메인 서비스. "여러 Stock에 걸친 원자적 차감"이라는 단일 엔티티에 속하지 않는 책임을   │
│ 담당.                                                                                                                                    │
│ - StockModel.deduct(): 실제 차감 로직은 엔티티 내부에 유지. 도메인 서비스는 "어떤 순서로, 어떤 조건에서" 차감할지를 조율.                │
│ - StockModel.quantity는 Long: 재고는 0이 될 수 있으므로 Quantity VO(>0)를 쓰지 않고 Long으로 유지. deduct()의 파라미터만 Quantity VO.    │
│                     