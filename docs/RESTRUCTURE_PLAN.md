# PLAN: TÁI CẤU TRÚC BOOKING-HOTEL-SYSTEM

> Microservices chuẩn + Clean Architecture, **không over-engineering**
> Tài liệu này là blueprint — chưa code, chỉ giải thích cấu trúc và lý do.

---

## 1. PHÂN TÍCH HIỆN TRẠNG

### 1.1 Những gì đang có

| Module | Trạng thái | Nhận xét |
|---|---|---|
| `auth-service` | ~100+ files, gần hoàn thiện | Phình to, chứa cả Ticket |
| `user-service` | Skeleton (12 files) | Trùng domain với auth-service |
| `api-gateway` | Hoạt động | OK |
| `keycloak-provider` | Hoạt động | OK |
| `booking-service` | **Chưa có** | Cần tạo |
| `payment-service` | **Chưa có** | Cần tạo |
| `ticket-service` | **Chưa có** | Đang nằm nhầm trong auth-service |

### 1.2 Vấn đề over-engineering phát hiện

**Vấn đề 1: Duplicate domain User/Role/Permission**
- `auth-service` có `UserEntity`, `RoleEntity`, `PermissionEntity`, `UserRepositoryPort`, `UserEntityMapper`
- `user-service` (skeleton) lại có y hệt: `User`, `Role`, `Permission`, `UserRepositoryPort`, `UserRepositoryAdapter`
- → Phải chọn 1 nơi sở hữu domain này.

**Vấn đề 2: Auth-service phình to (vi phạm SRP)**
- Auth-service hiện chứa: `SupportTicket`, `TicketController`, `CreateTicketUseCase`, `GetTicketsUseCase`, `ManageTicketUseCase`, `SupportTicketEntity`, `SupportTicketRepositoryAdapter`
- Ticket không liên quan auth → phải tách ra `ticket-service`.

**Vấn đề 3: Quá nhiều port interface mỏng (1-method interfaces)**
```
GetProfileUseCase, GetUserByIdUseCase, GetAllUsersUseCase,
UpdateProfileUseCase, UpdateUserUseCase, DeactivateUserUseCase,
AssignRoleUseCase, AdminResetPasswordUseCase, ChangePasswordUseCase,
GetTicketsUseCase, CreateTicketUseCase, ManageTicketUseCase
```
- Tách interface cho 1 method → 15+ file interface chỉ để gọi 1 method.
- Clean Architecture cho phép gộp các use case liên quan vào 1 interface theo **Aggregate** (User, Ticket, Session) thay vì theo **method**.

**Vấn đề 4: Filter chồng chéo**
- `ApiKeyFilter`, `IdempotencyFilter`, `TraceFilter`, `TokenAuthFilter` đang nằm trong auth-service
- Khi có 4 services backend → sẽ duplicate code này 4 lần
- → Phải có shared module (common-lib) chứa filter, exception, response format.

**Vấn đề 5: BaseException, ErrorCode, GlobalExceptionHandler lặp lại**
- Mỗi service sẽ cần cái này → nếu copy-paste sẽ drift version.

---

## 2. NGUYÊN TẮC TÁI CẤU TRÚC

### 2.1 Microservices chuẩn — không over-engineering

| Nên | Không nên |
|---|---|
| 1 service = 1 bounded context rõ ràng | Service làm nhiều việc (auth + ticket) |
| Mỗi service có DB riêng (hoặc schema riêng) | Share DB giữa các service |
| Giao tiếp qua HTTP (sync) hoặc Kafka (async) | Service gọi trực tiếp DB của service khác |
| Shared code qua common library (JAR) | Copy-paste code giữa các service |
| API contract rõ ràng (OpenAPI/Swagger) | Gọi nội bộ không có doc |

### 2.2 Clean Architecture — đơn giản hóa

**Bỏ:** `application/port/in/*UseCase` tách rời cho mỗi method.
**Giữ:** Cấu trúc `domain → application → infrastructure → presentation` nhưng gộp use case theo aggregate.

**Trước (over-engineered):**
```
application/port/in/
  ├── LoginUseCase.java          (1 method)
  ├── LogoutUseCase.java         (1 method)
  ├── GetProfileUseCase.java     (1 method)
  └── ... 15+ file interface
```

**Sau (gọn):**
```
application/service/
  ├── UserService.java           (login, logout, getProfile, updateProfile, changePassword, register, getUserById, getAllUsers, deactivateUser, assignRole, adminResetPassword)
  ├── TicketService.java         (createTicket, getMyTickets, getTicketById, assignTicket, resolveTicket, closeTicket)
  ├── SessionService.java        (revokeSession, revokeAllSessions, validateToken)
  └── ... (gộp theo aggregate)
```

Lý do: Use case interface chỉ có lợi khi có **nhiều implementation** (vd: `LoginUseCase` có impl keycloak + impl local). Trong project này, 1 use case = 1 impl duy nhất → interface thừa.

### 2.3 Domain ownership — chỉ 1 service sở hữu User

**Quyết định: `user-service` sở hữu User/Role/Permission** (CRUD profile, role assignment, RBAC lookup).

Auth-service **chỉ làm authentication**:
- Không lưu `users`, `roles`, `permissions` trong auth DB
- Khi login → gọi user-service (HTTP nội bộ) hoặc share DB schema `auth.users` qua view
- Hoặc: auth-service dùng Keycloak User Storage SPI (như hiện tại) để truy vấn user từ DB chung

**Đề xuất cuối cùng: Auth-service dùng Keycloak SPI như hiện tại (RemoteUserStorageProvider gọi /internal/users/verify). User-service sở hữu CRUD profile.**

```
user-service DB (schema: user_db)
  ├── users (id, username, email, status, ...)
  ├── roles
  ├── permissions
  ├── user_roles
  └── role_permissions

auth-service DB (schema: auth_db)
  ├── tokens (chỉ quản lý session, không lưu user)
  ├── system_params
  └── ... (chỉ những thứ thuộc auth)
```

→ Auth-service không cần `UserEntity`, `UserJpaRepository`, `UserRepositoryPort` nữa.

---

## 3. CẤU TRÚC MONOREPO ĐỀ XUẤT

### 3.1 Top-level

```
booking-system/
├── common-lib/                  ← Shared library (JAR) - code dùng chung
├── auth-service/                ← Port 8081 - Authentication only
├── user-service/                ← Port 8082 - User profile + RBAC
├── booking-service/             ← Port 8083 - Booking flow
├── payment-service/             ← Port 8084 - Payment processing
├── ticket-service/              ← Port 8085 - Support tickets
├── notification-service/        ← Port 8086 - Email/SMS (consume Kafka)
├── api-gateway/                 ← Port 8080 - Entry point
├── keycloak-provider/           ← KC SPI (giữ nguyên)
├── docker/
├── docker-compose.yml
├── .env
├── docs/
└── start-dev.bat
```

### 3.2 Vai trò từng service

| Service | Trách nhiệm | DB Schema | Giao tiếp |
|---|---|---|---|
| **api-gateway** | Routing, JWT verify, rate limit, CORS | Không có | HTTP |
| **auth-service** | Login/logout, token, 2FA, session | `auth_db` (tokens, system_params) | HTTP tới user-service, Kafka email event |
| **user-service** | CRUD user, role, permission, RBAC | `user_db` (users, roles, permissions) | HTTP từ auth-service, Kafka events |
| **booking-service** | Tạo/sửa/hủy booking, check room | `booking_db` (bookings, rooms) | HTTP tới payment (Feign), Kafka events |
| **payment-service** | Xử lý thanh toán (mock), lưu payment | `payment_db` (payments) | HTTP từ booking, Kafka events |
| **ticket-service** | Support ticket lifecycle | `ticket_db` (tickets) | Kafka events |
| **notification-service** | Gửi email/SMS, consume events | Không (hoặc log only) | Kafka consumer |

### 3.3 Tại sao cần `common-lib`?

Khi có 6 services backend, **không thể copy-paste**:
- Exception hierarchy (BaseException, ErrorCode, GlobalExceptionHandler)
- Filter (ApiKeyFilter, TraceFilter)
- Response format (ErrorResponse, PageResponse)
- Common util (MaskUtil, TimezoneConfig)
- Security config base

→ `common-lib` là Maven module (JAR), import qua `<dependency>` trong pom.xml của từng service.

```
common-lib/
├── pom.xml                      ← packaging: jar
└── src/main/java/com/booking/common/
    ├── exception/
    │   ├── BaseException.java
    │   ├── DomainException.java
    │   ├── ErrorCode.java        (interface, mỗi service impl riêng với prefix riêng)
    │   └── GlobalExceptionHandler.java
    ├── response/
    │   ├── ApiResponse.java      (success wrapper)
    │   └── ErrorResponse.java
    ├── security/
    │   ├── ApiKeyFilter.java
    │   ├── TraceFilter.java
    │   └── AuthContext.java      (helper lấy userId từ SecurityContext)
    ├── util/
    │   ├── MaskUtil.java
    │   └── TimezoneConfig.java
    └── config/
        └── CommonAutoConfig.java  (Spring auto-configuration)
```

Các service dùng:
```xml
<dependency>
    <groupId>com.booking</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 4. CẤU TRÚC TỪNG SERVICE

### 4.1 Cấu trúc chuẩn (áp dụng cho mọi service mới)

Mỗi service có **cùng layout** để team dễ navigate:

```
{service-name}/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/booking/{service}/
    │   ├── {Service}Application.java
    │   │
    │   ├── domain/                       ← Pure Java, không phụ thuộc framework
    │   │   ├── model/                    ← Entity, Value Object
    │   │   ├── enums/
    │   │   └── exception/                ← Chỉ exception riêng của domain này
    │   │
    │   ├── application/
    │   │   ├── service/                  ← Service interface + impl (gộp use case theo aggregate)
    │   │   │   ├── UserService.java
    │   │   │   ├── UserServiceImpl.java
    │   │   │   └── ...
    │   │   └── event/                    ← Domain event DTO (cho Kafka)
    │   │
    │   ├── infrastructure/
    │   │   ├── persistence/              ← JPA, Redis, Mapper
    │   │   │   ├── entity/
    │   │   │   ├── repository/
    │   │   │   ├── adapter/              ← Implement từ application
    │   │   │   └── mapper/
    │   │   ├── external/                 ← HTTP client, Kafka producer/consumer
    │   │   ├── security/                 ← Security config riêng (filter dùng chung từ common-lib)
    │   │   └── config/                   ← AppProperties, Bean config
    │   │
    │   └── presentation/
    │       ├── controller/
    │       ├── request/                  ← DTO Request (validation)
    │       ├── response/                 ← DTO Response
    │       └── advice/                   ← GlobalExceptionHandler (extend từ common-lib)
    │
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-docker.yml
        └── db/migration/                  ← Flyway (nếu có DB riêng)
```

### 4.2 Cải tiến so với cấu trúc hiện tại

| Hiện tại | Đề xuất | Lý do |
|---|---|---|
| `application/port/in/*UseCase.java` (15+ file) | `application/service/UserService.java` (interface + impl) | 1 aggregate = 1 service interface, gộp các method liên quan |
| `application/port/out/UserRepositoryPort.java` | Giữ nguyên | Output port vẫn có giá trị (test dễ, swap impl) |
| `infrastructure/security/filter/*Filter` | Filter common → common-lib, filter riêng → giữ service | Tránh duplicate 6 lần |
| `domain/exception/ErrorCode.java` | `ErrorCode` interface trong common-lib, mỗi service implement riêng | Mỗi service có prefix riêng (AUTH_, USR_, BOOK_, PAY_, TKT_) |
| `presentation/advice/GlobalExceptionHandler` | Trong common-lib (default), service có thể override nếu cần | Không lặp code |
| `BackendApplication.java` đặt ở root package | Đặt ở `com.booking.{service}` | Mỗi service có package riêng, tránh conflict |

---

## 5. CHI TIẾT TỪNG SERVICE

### 5.1 auth-service (sau khi tái cấu trúc)

**Loại bỏ:**
- Toàn bộ `UserEntity`, `RoleEntity`, `PermissionEntity`, `UserRepositoryPort`, `RoleRepositoryPort`, `UserEntityMapper`, `RoleEntityMapper`, `UserJpaRepository`, `RoleJpaRepository`, `UserRepositoryAdapter`, `RoleRepositoryAdapter`
- Toàn bộ `User`, `Role`, `Permission` domain model
- Toàn bộ User CRUD use case (GetUserById, GetAllUsers, UpdateUser, DeactivateUser, AssignRole, AdminResetPassword, ChangePassword, UpdateProfile, GetProfile)
- Toàn bộ `AdminController` (chuyển sang user-service)
- Toàn bộ SupportTicket (chuyển sang ticket-service)
- `CreateTicketUseCase`, `GetTicketsUseCase`, `ManageTicketUseCase`, `SupportTicketService`, `SupportTicketEntity`, `SupportTicketRepositoryAdapter`

**Giữ lại:**
- `AuthService` (login, logout, register)
- `TokenService` (tạo, validate, revoke token)
- `SessionService` (revoke session)
- `TwoFactorService` (2FA)
- `PasswordService` (hash, verify)
- `JwtService` (JWT generate, parse)
- `SystemParamService` (load config từ DB)
- `KeycloakGateway`
- `AuthController`, `InternalController`
- `TokenEntity`, `TokenRepositoryAdapter`, `SystemParamEntity`, `SystemParamRepositoryAdapter`
- `TokenAuthFilter` (vì auth-service cần verify token cho chính nó)

**Bổ sung (nếu cần):**
- Khi login → gọi `user-service` (qua Feign client hoặc WebClient) để lấy thông tin user, roles
- Hoặc: giữ cách cũ, dùng Keycloak SPI để truy vấn trực tiếp DB user (nếu schema chung)

**Quyết định cần làm rõ:** Auth-service lấy user info từ đâu khi login?
- **Phương án A:** Auth-service gọi HTTP `user-service /internal/users/{username}` → sync, cần user-service chạy
- **Phương án B:** Auth-service share schema `user_db.users` (read-only) → không cần gọi HTTP
- **Phương án C:** Dùng Keycloak SPI để KC tự truy vấn user_db khi verify password (như hiện tại)

→ **Đề xuất: giữ Phương án C** (đã hoạt động). Auth-service không cần User entity vì Keycloak lo phần đó.

### 5.2 user-service

**Mở rộng từ skeleton hiện tại:**
- Thêm `UserService` (CRUD: getById, getByUsername, getAll, update, deactivate, assignRole, adminResetPassword, changePassword, getProfile, updateProfile)
- Thêm `RoleService`, `PermissionService`
- Thêm `UserController` (`/users`, `/users/{id}`, `/users/me`, `/admin/users/...`)
- Thêm migration Flyway: `V1__init_user_schema.sql`, `V2__seed_roles_permissions.sql`
- Thêm `RoleEntityMapper`, `UserEntityMapper` (nếu skeleton chưa có)
- Bỏ `UserRepositoryPort` skeleton duplicate, giữ interface clean

**Lưu ý:** User CRUD yêu cầu auth → JWT filter (từ common-lib) sẽ verify token, lấy userId từ claim, gọi `UserService.getById(...)` để check quyền.

### 5.3 booking-service (tạo mới)

**Domain model:**
- `Room` (id, code, type, pricePerNight, status, maxOccupancy)
- `Booking` (id, userId, roomId, checkInDate, checkOutDate, totalAmount, status, createdAt)
- `BookingStatus` enum: `PENDING, CONFIRMED, CANCELLED, CHECKED_IN, CHECKED_OUT, COMPLETED, FAILED`

**Use case:**
- `BookingService`:
  - `createBooking(userId, roomId, checkIn, checkOut)` → gọi payment-service, lưu booking
  - `cancelBooking(bookingId, userId)` → check ownership, refund (call payment)
  - `getBookingById(bookingId)`
  - `getMyBookings(userId, pageable)`
  - `confirmBooking(bookingId)` (sau khi payment success — consume Kafka event)
  - `updateBookingStatus(...)` (nội bộ)

**Repository:**
- `BookingRepositoryPort` (output port)
- `RoomRepositoryPort`

**External:**
- `PaymentClient` (Feign client gọi payment-service)
- `KafkaConsumer` listen `payment-events` topic → update booking status

**Controller:**
- `POST /bookings` — tạo booking
- `GET /bookings/{id}` — chi tiết
- `GET /bookings/me` — lịch sử của tôi
- `DELETE /bookings/{id}` — hủy

**DB:** `booking_db`
- `rooms`, `bookings`, `booking_history` (audit)

### 5.4 payment-service (tạo mới)

**Domain model:**
- `Payment` (id, bookingId, userId, amount, method, status, transactionId, createdAt)
- `PaymentStatus`: `PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED`
- `PaymentMethod`: `CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, MOCK`

**Use case:**
- `PaymentService`:
  - `processPayment(bookingId, userId, amount, method)` → mock xử lý, trả về status
  - `refundPayment(paymentId)` → mock refund
  - `getPaymentByBookingId(bookingId)`

**Repository:**
- `PaymentRepositoryPort`

**External:**
- Publish `PaymentProcessedEvent` lên Kafka topic `payment-events`

**Controller:**
- `POST /payments` — tạo payment (internal, gọi từ booking)
- `GET /payments/{id}` — chi tiết
- `POST /payments/{id}/refund` — refund

**DB:** `payment_db`
- `payments`, `payment_history`

### 5.5 ticket-service (tách từ auth-service)

**Domain model:**
- `SupportTicket` (id, userId, title, description, priority, status, assignedTo, createdAt)
- `TicketStatus`: `OPEN, IN_PROGRESS, RESOLVED, CLOSED`
- `TicketPriority`: `LOW, MEDIUM, HIGH, URGENT`

**Use case:**
- `TicketService`:
  - `createTicket(userId, title, desc, priority)`
  - `getMyTickets(userId, pageable)`
  - `getTicketById(ticketId)`
  - `assignTicket(ticketId, staffId)` (admin)
  - `updateTicketStatus(ticketId, status)` (staff)
  - `closeTicket(ticketId)`

**Repository:**
- `TicketRepositoryPort`

**Controller:**
- `POST /tickets` — user tạo
- `GET /tickets/me` — user xem của mình
- `GET /tickets/{id}` — chi tiết
- `PATCH /tickets/{id}/assign` — admin assign
- `PATCH /tickets/{id}/status` — staff update

**DB:** `ticket_db`
- `tickets`, `ticket_comments` (nếu cần)

### 5.6 notification-service (tạo mới — tùy chọn)

**Lý do tách:** Email/SMS gửi qua Kafka event, không nên để mỗi service tự gửi.

**Chức năng:**
- Consume tất cả event từ Kafka (user.registered, booking.confirmed, payment.success, ticket.created...)
- Gửi email qua SMTP (Mailgun, SendGrid mock)
- Log notification sent

**Hiện tại auth-service có `EmailConsumer` + `EmailProducer` → tách ra service riêng.**

---

## 6. AUTHENTICATION & AUTHORIZATION FLOW (sau tái cấu trúc)

### 6.1 Login flow

```
[FE] POST /api/auth/login {username, password}
         │
         ▼
[API Gateway :8080]  ← X-API-Key filter, CORS, route /api/auth/** → auth-service
         │
         ▼
[Auth Service :8081]
  1. AuthController.login(req)
  2. AuthService.login(username, password)
     a. KeycloakGateway.authenticate(username, password)
        → POST http://keycloak:8180/realms/booking/protocol/openid-connect/token
        → KC dùng Custom SPI → truy vấn user_db.users (qua JDBC hoặc qua auth-service /internal/users/verify)
        → Trả OK/FAIL
     b. Nếu OK → load user roles (gọi user-service HOẶC query thẳng DB user_db qua SPI)
     c. Check 2FA enabled? → tạo mfa_session, trả MFA response
     d. Tạo JWT (username, userId, roles[], jti, exp)
     e. Lưu token hash vào auth_db.tokens
     f. Cache token vào Redis
     g. Trả LoginResponse {token, roles, expiresIn}
```

### 6.2 Request có auth

```
[FE] GET /api/bookings/me
     Headers: Authorization: Bearer <jwt>, X-API-Key: xxx
         │
         ▼
[API Gateway :8080]
  1. ApiKeyFilter (common-lib) — check X-API-KEY
  2. JwtAuthFilter (gateway) — verify JWT signature + exp, KHÔNG check DB
  3. Add header X-User-Id, X-User-Roles vào request
  4. Route /api/bookings/** → booking-service
         │
         ▼
[Booking Service :8083]
  1. JwtAuthFilter (common-lib) — verify JWT, lấy userId từ claim
  2. Set SecurityContext
  3. Controller gọi BookingService.getMyBookings(userId)
```

**Lưu ý quan trọng:** Gateway chỉ verify JWT signature + expiry (nhanh, stateless). Service backend KHÔNG cần gọi auth-service để verify token mỗi request → nhanh hơn.

Nếu cần revoke token ngay lập tức (như "1 phiên duy nhất") → check Redis blacklist:
```java
// Trong JwtAuthFilter (common-lib)
if (redisTemplate.hasKey("blacklist:" + jti)) {
    throw new TokenException("Token revoked");
}
```

### 6.3 Token storage & 1-phiên-duy-nhất

- Auth-service tạo token → lưu `auth_db.tokens` (token_hash, user_id, jti, revoked, created_at)
- Khi login mới → UPDATE tokens SET revoked=true WHERE user_id=? AND revoked=false
- Khi logout → UPDATE tokens SET revoked=true WHERE jti=?
- Filter check Redis `token:{userId}` để biết token hiện tại, nếu token gửi lên ≠ token trong Redis → 401

---

## 7. GIAO TIẾP GIỮA CÁC SERVICE

### 7.1 Sync (HTTP qua Feign client) — khi cần response ngay

| From | To | Use case |
|---|---|---|
| auth-service | user-service | Verify credentials (nếu không dùng KC SPI) |
| booking-service | payment-service | Process payment khi tạo booking |
| api-gateway | Tất cả | Route request |

### 7.2 Async (Kafka) — khi không cần response ngay

| Topic | Producer | Consumer | Event |
|---|---|---|---|
| `user-events` | user-service | auth-service, notification-service | UserRegistered, UserDeactivated, RoleAssigned |
| `booking-events` | booking-service | payment-service, notification-service | BookingCreated, BookingCancelled |
| `payment-events` | payment-service | booking-service, notification-service | PaymentProcessed, PaymentFailed, Refunded |
| `ticket-events` | ticket-service | notification-service | TicketCreated, TicketAssigned, TicketResolved |
| `email-events` | Tất cả | notification-service | SendEmailCommand |

**Lý do dùng Kafka:** Booking service không cần block chờ payment xử lý xong → publish event, payment consumer xử lý, sau đó publish `payment-events` để booking update status.

### 7.3 Internal API (service-to-service qua gateway)

- Các service gọi nhau **KHÔNG qua gateway** → dùng Feign client với URL nội bộ (http://user-service:8082)
- Trừ khi muốn enforce rate limit / audit log tập trung
- Auth bằng service-to-service token (JWT riêng, claim `service: booking-service`)

---

## 8. DATABASE STRATEGY

### 8.1 Mỗi service 1 schema (multi-schema trong 1 DB instance)

Lý do: Dev/test dễ, prod có thể tách DB instance từng service sau.

```
postgres instance
├── user_db       (user-service owns)
├── auth_db       (auth-service owns)
├── booking_db    (booking-service owns)
├── payment_db    (payment-service owns)
└── ticket_db     (ticket-service owns)
```

### 8.2 Flyway migrations

Mỗi service có Flyway riêng, migrations nằm trong `src/main/resources/db/migration/`. Không share file migration giữa các service.

### 8.3 Data isolation rule

- **KHÔNG BAO GIỜ** join DB cross-schema trong code service
- **KHÔNG BAO GIỜ** truy cập DB của service khác
- Nếu cần data của service khác → gọi HTTP hoặc consume Kafka event

---

## 9. DOCKER COMPOSE (sau tái cấu trúc)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    # Tạo schemas: user_db, auth_db, booking_db, payment_db, ticket_db

  redis:
    # Giữ nguyên

  kafka:
    # Giữ nguyên + tạo topics

  keycloak:
    # Giữ nguyên

  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
    depends_on: [auth-service, user-service, booking-service, payment-service, ticket-service]

  auth-service:
    build: ./auth-service
    expose: ["8081"]
    depends_on: [postgres, redis, kafka, keycloak]

  user-service:
    build: ./user-service
    expose: ["8082"]

  booking-service:
    build: ./booking-service
    expose: ["8083"]
    depends_on: [postgres, kafka, payment-service]

  payment-service:
    build: ./payment-service
    expose: ["8084"]
    depends_on: [postgres, kafka]

  ticket-service:
    build: ./ticket-service
    expose: ["8085"]
    depends_on: [postgres, kafka]

  notification-service:
    build: ./notification-service
    expose: ["8086"]
    depends_on: [kafka]
```

---

## 10. MIGRATION ROADMAP (thứ tự thực hiện)

### Phase 1: Nền tảng chung (tuần 1)
1. Tạo `common-lib` (exception, response, filter, util)
2. Migrate auth-service dùng common-lib
3. Refactor auth-service: bỏ User/Role/Permission, bỏ SupportTicket

### Phase 2: User service (tuần 2)
4. Hoàn thiện user-service (skeleton → đầy đủ CRUD + controller)
5. Update auth-service gọi user-service qua Feign (hoặc giữ KC SPI)
6. Test E2E: register → login → get profile

### Phase 3: Booking + Payment (tuần 3-4)
7. Tạo booking-service (domain, repository, service, controller)
8. Tạo payment-service
9. Wire Feign client booking → payment
10. Test flow: create booking → process payment → confirm

### Phase 4: Ticket (tuần 5)
11. Tạo ticket-service (port từ auth-service)
12. Di chuyển `SupportTicket*` từ auth-service sang ticket-service
13. Update API docs

### Phase 5: Notification + Polish (tuần 6)
14. Tạo notification-service
15. Wire Kafka events
16. Update docker-compose
17. Test E2E toàn hệ thống
18. JMeter load test

---

## 11. CHECKLIST VERIFY "KHÔNG OVER-ENGINEERING"

Trước khi thêm abstraction mới, tự hỏi:

- [ ] Interface này có > 1 implementation không? Nếu không → bỏ interface, dùng class trực tiếp
- [ ] Abstraction này giúp test dễ hơn không? Nếu không → bỏ
- [ ] Code có bị duplicate giữa các service không? Nếu có → đưa vào common-lib
- [ ] Service này có 1 bounded context rõ ràng không? Nếu không → tách
- [ ] Có thể giải thích trong 1 câu service này làm gì không? Nếu không → tách nhỏ hơn

**Rule of thumb:** Ưu tiên đơn giản. Chỉ abstract khi thực sự cần. Microservices ≠ nhiều file.

---

## 12. TÓM TẮT QUYẾT ĐỊNH

| Câu hỏi | Quyết định |
|---|---|
| Có cần user-service riêng không? | **Có** — auth-service không nên chứa User CRUD |
| Auth-service có nên giữ User/Role entity? | **Không** — Keycloak SPI truy vấn user_db trực tiếp |
| Ticket có nên ở auth-service? | **Không** — tách sang ticket-service |
| Filter chung đặt ở đâu? | **common-lib** — mỗi service import và dùng |
| Service giao tiếp kiểu gì? | **Sync qua Feign HTTP** (khi cần response), **Async qua Kafka** (khi không cần) |
| Có cần notification-service riêng? | **Có** (khuyến nghị) — để email không lẫn vào business logic |
| Có cần OpenTelemetry không? | **Có** nhưng làm sau (Phase 5) — ưu tiên business logic trước |
| Ai sở hữu schema `users`? | **user-service** — các service khác không truy cập trực tiếp |
| Có dùng Saga pattern không? | **Chưa** — giai đoạn đầu dùng simple event-driven + rollback thủ công |

---

## 13. FILE CẦN TẠO / SỬA

### Tạo mới:
- `common-lib/` (toàn bộ)
- `booking-service/`
- `payment-service/`
- `ticket-service/`
- `notification-service/` (optional)

### Sửa / Xóa trong auth-service:
- Xóa: `User`, `Role`, `Permission` (domain + entity + port + adapter + jpa repo)
- Xóa: `SupportTicket*` (chuyển sang ticket-service)
- Xóa: 15+ `*UseCase` interface → gộp vào service
- Sửa: import từ `common-lib` cho exception, filter, response
- Sửa: `SecurityConfig` dùng common filter
- Sửa: `pom.xml` thêm dependency `common-lib`

### Giữ nguyên:
- `keycloak-provider/` 
- `docker/`, `docker-compose.yml`, `.env`
- `frontend/`

---

*Tài liệu này là Single Source of Truth cho việc tái cấu trúc. Mọi thay đổi về kiến trúc phải cập nhật file này.*
