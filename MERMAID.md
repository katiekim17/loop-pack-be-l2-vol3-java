# Flow Diagrams

## 1. 회원가입 (POST /api/v1/members)

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service
    participant DB

    Client->>Controller: POST /api/v1/members (SignUpRequest)
    Controller->>Facade: signupMember(request)
    Facade->>Facade: Request to MemberModel 변환
    Facade->>Service: saveMember(memberModel)
    Service->>DB: findByLoginId (중복 체크)
    DB-->>Service: Optional.empty()
    Service->>Service: passwordEncoder.encode()
    Service->>DB: save(memberModel)
    DB-->>Service: savedMember
    Service-->>Facade: MemberModel
    Facade->>Facade: MemberModel to MemberInfo 변환
    Facade-->>Controller: MemberInfo
    Controller-->>Client: 201 Created (SignUpResponse)
```

### 예외 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service
    participant DB

    Client->>Controller: POST /api/v1/members (중복 ID)
    Controller->>Facade: signupMember(request)
    Facade->>Service: saveMember(memberModel)
    Service->>DB: findByLoginId
    DB-->>Service: Optional.of(existingMember)
    Service-->>Controller: CoreException (CONFLICT)
    Controller-->>Client: 409 Conflict
```

---

## 2. 내 정보 조회 (GET /api/v1/members/me)

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service
    participant DB

    Client->>Controller: GET /api/v1/members/me
    Note over Client,Controller: Headers: X-Loopers-LoginId, X-Loopers-LoginPw
    Controller->>Facade: getMyInfo(loginId, password)
    Facade->>Service: authenticate(loginId, password)
    Service->>DB: findByLoginId
    DB-->>Service: MemberModel
    Service->>Service: passwordEncoder.matches()
    Service-->>Facade: MemberModel (인증 성공)
    Facade->>Facade: MemberModel to MemberInfo 변환 (이름 마스킹)
    Facade-->>Controller: MemberInfo
    Controller-->>Client: 200 OK (MemberInfoResponse)
```

### 예외 흐름 - 인증 실패

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service
    participant DB

    Client->>Controller: GET /api/v1/members/me (틀린 비밀번호)
    Controller->>Facade: getMyInfo(loginId, wrongPassword)
    Facade->>Service: authenticate(loginId, wrongPassword)
    Service->>DB: findByLoginId
    DB-->>Service: MemberModel
    Service->>Service: passwordEncoder.matches() = false
    Service-->>Controller: CoreException (UNAUTHORIZED)
    Controller-->>Client: 401 Unauthorized
```

---

## 3. 비밀번호 변경 (PATCH /api/v1/members/me/password)

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service
    participant DB

    Client->>Controller: PATCH /api/v1/members/me/password
    Note over Client,Controller: Headers: X-Loopers-LoginId, X-Loopers-LoginPw
    Note over Client,Controller: Body: oldPassword, newPassword
    Controller->>Facade: changePassword(loginId, headerPw, oldPw, newPw)
    Facade->>Service: authenticate(loginId, headerPw)
    Service->>DB: findByLoginId
    DB-->>Service: MemberModel
    Service->>Service: passwordEncoder.matches(headerPw)
    Service-->>Facade: 인증 성공
    Facade->>Facade: new MemberModel(loginId, oldPw)
    Facade->>Service: changePassword(memberModel, newPw)
    Service->>Service: passwordEncoder.matches(oldPw) 검증
    Service->>Service: newPw != oldPw 검증
    Service->>Service: validatePassword(newPw) 규칙 검증
    Service->>Service: passwordEncoder.encode(newPw)
    Service->>DB: Dirty Checking (자동 저장)
    Service-->>Facade: void
    Facade-->>Controller: void
    Controller-->>Client: 200 OK
```

### 예외 흐름 - Body의 기존 비밀번호 불일치

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service

    Client->>Controller: PATCH (헤더 인증 OK, Body oldPw 틀림)
    Controller->>Facade: changePassword(...)
    Facade->>Service: authenticate() 성공
    Facade->>Service: changePassword(wrongOldPw, newPw)
    Service->>Service: passwordEncoder.matches(wrongOldPw) = false
    Service-->>Controller: CoreException (UNAUTHORIZED)
    Controller-->>Client: 401 Unauthorized
```

### 예외 흐름 - 새 비밀번호가 기존과 동일

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Facade
    participant Service

    Client->>Controller: PATCH (newPw == oldPw)
    Controller->>Facade: changePassword(...)
    Facade->>Service: authenticate() 성공
    Facade->>Service: changePassword(oldPw, samePassword)
    Service->>Service: oldPw 검증 성공
    Service->>Service: newPw == oldPw 체크
    Service-->>Controller: CoreException (BAD_REQUEST)
    Controller-->>Client: 400 Bad Request
```