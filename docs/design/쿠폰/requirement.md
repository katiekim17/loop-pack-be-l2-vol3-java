# 🎟 쿠폰 기능 상세 정의

---

## FR-1. 쿠폰 템플릿 등록 (어드민)

**사용자 스토리:**  
운영자는 어드민 페이지에서 쿠폰 템플릿을 등록해 사용자에게 발급할 쿠폰의 할인 규칙을 정의할 수 있다.

**API 명세:**
```
POST /api-admin/v1/coupons
Authorization: LDAP Required
```

**요청 Body:**
```json
{
  "name": "신규가입 10% 할인",
  "type": "RATE",
  "value": 10,
  "minOrderAmount": 10000,
  "expiredAt": "2026-12-31T23:59:59"
}
```

**상세 동작:**
1. LDAP 인증된 운영자만 등록 가능
2. 요청 필드 유효성 검증 후 쿠폰 템플릿 저장

**유효성 검증:**

| 필드 | 규칙 |
|------|------|
| `name` | 필수, 공백 불가 |
| `type` | 필수, `FIXED` 또는 `RATE` |
| `value` | 필수, `FIXED`: 1 이상 / `RATE`: 1~100 |
| `minOrderAmount` | 선택, 0 이상 |
| `expiredAt` | 필수, 현재 시각 이후 |

**정책:**
- `type`, `value`는 등록 후 수정 불가 (변경이 필요한 경우 새 템플릿 등록)
- 삭제는 Soft delete 방식 (`deletedAt` 설정)

**에러 처리:**
- 유효성 검증 실패 → `400 Bad Request`

---

## FR-2. 쿠폰 템플릿 수정 (어드민)

**사용자 스토리:**  
운영자는 등록된 쿠폰 템플릿의 이름, 최소 주문 금액, 만료 일시를 수정할 수 있다.

**API 명세:**
```
PUT /api-admin/v1/coupons/{couponId}
Authorization: LDAP Required
```

**상세 동작:**
1. LDAP 인증된 운영자만 수정 가능
2. `name`, `minOrderAmount`, `expiredAt`만 수정 가능
3. `type`, `value` 변경 시도 시 에러 반환

**정책:**
- 삭제된 쿠폰(`deletedAt != null`) 수정 불가
- 할인 타입/값은 불변 (`COUPON_TYPE_IMMUTABLE`)

**에러 처리:**
- 존재하지 않거나 삭제된 쿠폰 → `404 Not Found`
- `type` 또는 `value` 수정 시도 → `400 Bad Request`

---

## FR-3. 쿠폰 템플릿 삭제 (어드민)

**사용자 스토리:**  
운영자는 더 이상 발급하지 않을 쿠폰 템플릿을 삭제할 수 있다.

**API 명세:**
```
DELETE /api-admin/v1/coupons/{couponId}
Authorization: LDAP Required
```

**상세 동작:**
1. LDAP 인증된 운영자만 삭제 가능
2. Soft delete 처리 (`deletedAt` = 현재 시각)
3. 삭제된 템플릿은 발급 불가 상태로 전환

**정책:**
- 삭제된 쿠폰은 발급 API에서 `404` 처리
- 이미 발급된 `UserCoupon`은 영향 없음 (기존 발급 건은 유효)

**에러 처리:**
- 이미 삭제된 쿠폰 재삭제 → `404 Not Found`

---

## FR-4. 쿠폰 템플릿 목록/상세 조회 (어드민)

**사용자 스토리:**  
운영자는 등록된 쿠폰 템플릿 목록과 상세 정보를 확인할 수 있다.

**API 명세:**
```
GET /api-admin/v1/coupons?page=0&size=20
GET /api-admin/v1/coupons/{couponId}
Authorization: LDAP Required
```

**상세 동작:**
1. 삭제된 쿠폰(`deletedAt != null`)은 목록에서 제외
2. 목록은 페이지네이션 지원

**에러 처리:**
- 삭제된 쿠폰 상세 조회 → `404 Not Found`

---

## FR-5. 쿠폰 발급 내역 조회 (어드민)

**사용자 스토리:**  
운영자는 특정 쿠폰 템플릿에 대한 발급 내역을 확인할 수 있다.

**API 명세:**
```
GET /api-admin/v1/coupons/{couponId}/issues?page=0&size=20
Authorization: LDAP Required
```

**상세 동작:**
1. 해당 쿠폰 템플릿에 발급된 `UserCoupon` 목록 조회
2. 발급 일시 내림차순 정렬
3. 페이지네이션 지원

---

## FR-6. 쿠폰 발급 요청 (대고객)

**사용자 스토리:**  
사용자는 쿠폰 발급 버튼을 눌러 자신의 계정에 쿠폰을 발급받을 수 있다.

**API 명세:**
```
POST /api/v1/coupons/{couponId}/issue
Authorization: Required
```

**상세 동작:**
1. 인증된 사용자만 발급 가능
2. `couponId`에 해당하는 쿠폰 템플릿 조회 (없거나 삭제된 경우 실패)
3. 쿠폰 만료 여부 확인 (`expiredAt` 이미 지난 경우 실패)
4. 동일 사용자의 중복 발급 여부 확인 (이미 발급된 경우 실패)
5. `UserCoupon` 생성 후 저장, 상태 = `AVAILABLE`

**정책:**
- 사용자당 동일 쿠폰 1회만 발급 가능 — `Unique(user_id, coupon_id)`
- 발급은 동기 처리 (즉시 응답)

**에러 처리:**
- 존재하지 않거나 삭제된 쿠폰 → `404 Not Found`
- 이미 만료된 쿠폰 → `400 Bad Request`
- 동일 쿠폰 중복 발급 → `400 Bad Request` (`COUPON_ALREADY_ISSUED`)

---

## FR-7. 내 쿠폰 목록 조회 (대고객)

**사용자 스토리:**  
사용자는 자신이 보유한 쿠폰 목록과 각 쿠폰의 사용 가능 여부를 확인할 수 있다.

**API 명세:**
```
GET /api/v1/users/me/coupons
Authorization: Required
```

**상세 동작:**
1. 인증된 사용자만 조회 가능
2. 로그인한 사용자의 `UserCoupon` 목록 조회
3. 각 쿠폰의 상태(`AVAILABLE` / `USED` / `EXPIRED`) 포함 반환

**반환 정보:**
```json
{
  "content": [
    {
      "userCouponId": 10,
      "couponName": "신규가입 10% 할인",
      "type": "RATE",
      "value": 10,
      "minOrderAmount": 10000,
      "expiredAt": "2026-12-31T23:59:59",
      "status": "AVAILABLE",
      "issuedAt": "2026-03-01T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

## FR-8. 주문 시 쿠폰 적용 (주문 연동)

**사용자 스토리:**  
사용자는 주문 시 보유한 쿠폰을 적용해 할인된 금액으로 결제할 수 있다.

**API 명세:**
```
POST /api/v1/orders
Authorization: Required
```

**요청 Body:**
```json
{
  "items": [
    { "productOptionId": 1, "quantity": 2 },
    { "productOptionId": 3, "quantity": 1 }
  ],
  "userCouponId": 42
}
```
> `userCouponId`는 선택 항목 (미적용 시 생략 가능)

**상세 동작:**
1. 쿠폰 미적용 시 → 정상 주문 금액으로 처리
2. 쿠폰 적용 시 유효성 검증:
    - `UserCoupon`이 존재하지 않는 경우 → 주문 실패
    - `UserCoupon`의 소유자가 요청 사용자와 다른 경우 → 주문 실패
    - `UserCoupon.status != AVAILABLE` → 주문 실패
    - `coupon.expiredAt`이 지난 경우 → `UserCoupon.status`를 `EXPIRED`로 변경 후 주문 실패
    - 주문 금액이 `coupon.minOrderAmount` 미만인 경우 → 주문 실패
3. 검증 통과 시 할인 금액 계산:
    - `FIXED`: `주문금액 - value` (최솟값 0원)
    - `RATE`: `주문금액 × (1 - value / 100)` (소수점 이하 버림)
4. `OrderItem` 스냅샷에 가격 정보 저장:
    - `orderPrice`: 쿠폰 적용 전 금액
    - `discountAmount`: 할인 금액 (쿠폰 미적용 시 0)
    - `finalPrice`: 최종 결제 금액
5. `UserCoupon.status` → `USED`, `usedAt` = 현재 시각으로 업데이트

**정책:**
- 주문 1건당 쿠폰 최대 1장 적용
- 쿠폰 상태 변경과 주문 생성은 하나의 트랜잭션으로 처리 (강한 일관성)
- 쿠폰 적용 실패 시 주문 전체 실패 (All-or-Nothing)

**에러 처리:**
- 존재하지 않는 발급 쿠폰 → `404 Not Found`
- 타 유저 소유 쿠폰 → `400 Bad Request` (`COUPON_OWNER_MISMATCH`)
- 이미 사용/만료된 쿠폰 → `400 Bad Request` (`COUPON_NOT_AVAILABLE`)
- 최소 주문 금액 미충족 → `400 Bad Request` (`COUPON_MIN_ORDER_AMOUNT_NOT_MET`)

---

## 비기능 요구사항

### NFR-1. 성능
- 쿠폰 발급/조회 API는 200ms 이내 응답 목표
- 주문 시 쿠폰 검증은 주문 트랜잭션 내에서 처리하므로 단순하게 유지

### NFR-2. 데이터 일관성
- 쿠폰 발급 데이터는 강한 일관성 (DB Unique 제약)
- 쿠폰 상태 변경(`USED`)과 주문 생성은 동일 트랜잭션 보장

### NFR-3. 보안
- 모든 API는 인증 필수
- 타 유저 쿠폰 사용 시도는 명시적 에러 처리

---

## 결정된 제약사항 및 전제조건

### 데이터 제약
- 사용자 1명당 동일 쿠폰 1회만 발급 가능
    - DB Unique Index: `(user_id, coupon_id)`
- 쿠폰 템플릿의 `type`, `value`는 등록 후 불변

### 아키텍처 결정
- **동기 처리:** 쿠폰 발급, 쿠폰 상태 변경, 주문 연동
- **트랜잭션 범위:** 쿠폰 USED 처리 + 주문 생성은 단일 트랜잭션
- **Soft delete:** 쿠폰 템플릿 삭제 시 `deletedAt` 설정, 기 발급 쿠폰은 유효

### 에러 코드 정의

| 에러 코드 | HTTP Status | 설명 |
|-----------|-------------|------|
| `COUPON_NOT_FOUND` | 404 | 쿠폰 템플릿이 존재하지 않거나 삭제됨 |
| `COUPON_EXPIRED` | 400 | 만료된 쿠폰으로 발급 또는 주문 시도 |
| `COUPON_ALREADY_ISSUED` | 400 | 동일 사용자에게 이미 발급된 쿠폰 |
| `COUPON_NOT_AVAILABLE` | 400 | USED 또는 EXPIRED 상태의 발급 쿠폰 사용 시도 |
| `COUPON_OWNER_MISMATCH` | 400 | 타 유저 소유 쿠폰 사용 시도 |
| `COUPON_MIN_ORDER_AMOUNT_NOT_MET` | 400 | 최소 주문 금액 미충족 |
| `USER_COUPON_NOT_FOUND` | 404 | 발급 쿠폰이 존재하지 않음 |
| `COUPON_TYPE_IMMUTABLE` | 400 | 쿠폰 타입 또는 할인값 수정 시도 |

### 향후 확장 고려사항
- 쿠폰 발급 수량 제한 (선착순 쿠폰)
- 특정 상품/카테고리 전용 쿠폰
- 중복 발급 허용 쿠폰 타입 추가

---

## 주요 시나리오별 정합성 처리

#### 쿠폰 만료 처리
**현재 (v1):**
- 만료 여부는 주문 시점에 실시간 검증
- 만료 확인 시 `UserCoupon.status` → `EXPIRED` 즉시 변경

**잠재 리스크:**
- `AVAILABLE` 상태이지만 실제로 만료된 쿠폰이 목록에 노출될 수 있음

**해결 방안:**
- 내 쿠폰 목록 조회 시 `expiredAt` 기준으로 실시간 상태 계산하여 응답
- 향후 배치를 통한 만료 상태 일괄 업데이트 고려

**설계 시 주의사항:**
- 쿠폰 상태(`status`)는 주문 시점에 최종 검증하는 것이 기준
- 목록 노출 상태는 UX 편의를 위한 참고값으로 사용