# PLAN: ADMIN UI MỞ RỘNG (Part A) + KEYCLOAK MOCK (Part B)

> Ngày lập: 2026-06-22
> Cập nhật: 2026-06-22 (v2 — sau feedback)
> Phạm vi: Frontend Angular (booking-admin-ui) — không code, chỉ plan
> Lựa chọn của user:
> - Tên = Keycloak `firstName + lastName`
> - A.5 + A.6 dùng chung data, 2 view khác nhau
> - Mock data = **cache (in-memory + sessionStorage)**
> - Keycloak = **toggle "Đăng nhập nhanh qua Keycloak"** bên cạnh form Angular
> - A.1, A.2 OK · A.4 manual workflow · A.5/A.6 OK · theme custom nhưng phải fallback được về default
> - **Xác nhận: code hết Part A trước, rồi mới hướng dẫn Keycloak**

---

## 🅰️ PART A — ADMIN SIDEBAR MỞ RỘNG

### A.0 Sidebar menu (file: `layouts/admin-layout/admin-layout.component.html`)

**Quyết định:** Menu gọn, đúng nghiệp vụ. Cấu trúc `nz-menu-group` + `nz-menu-item` đang có — chỉ điều chỉnh nhóm và điều kiện hiển thị theo role.

**Sidebar menu (menu items được lọc theo role, xem bảng phân quyền bên dưới):**

```
📊 Tổng quan
└─ Dashboard                                          [admin, host]

👤 Quản lý tài khoản
└─ Tài khoản          → /admin/users                  [admin]
└─ Phân quyền         → /admin/roles                  [admin]

🏨 Quản lý khách sạn
└─ Khách sạn          → /admin/hotels                 [admin, host]
└─ Phòng khách sạn    → /admin/hotel-rooms            [admin, host] (A.6)
└─ Loại phòng         → /admin/room-types             [admin, host]
└─ Phòng              → /admin/rooms                  [admin, host] (A.5)

📅 Đặt phòng
└─ Đơn đặt phòng      → /admin/bookings               [admin, host] (A.4)
└─ Thanh toán         → /admin/payments               [admin, host]

🎟 Hỗ trợ
└─ Phiếu hỗ trợ      → /admin/tickets                 [admin, host, user] (A.3)

[Đăng xuất]
```

**Cách implement điều kiện hiển thị theo role:**
- Dùng `*ngIf="hasRole('ADMIN')"` trên từng `nz-menu-item` (đã có helper `auth.hasRole()` / `auth.hasAnyRole()`)
- Group `Quản lý tài khoản` chỉ hiện với admin
- Group `Quản lý khách sạn` + `Đặt phòng` + `Thanh toán` hiện với admin và host
- `Phiếu hỗ trợ` hiện với cả 3 role
- User thường vào `/user/*` (booking, profile, tickets) — không thấy sider admin này

**Lý do A.5 + A.6 chung data:**
- A.5 (Phòng) — flat table, thao tác nhanh, filter, sửa hàng loạt
- A.6 (Phòng khách sạn) — master-detail, group theo hotel, dễ nhìn theo khách sạn
- Cùng `sessionStorage` key `mock_rooms` → CRUD ở view nào thì view kia update real-time qua `BehaviorSubject`

---

### A.1 — Tài khoản (Account)

**Files sửa:**
- `frontend/booking-admin-ui/src/app/layouts/admin/users/users.ts`
- `frontend/booking-admin-ui/src/app/layouts/admin/users/users.html`
- `frontend/booking-admin-ui/src/app/layouts/admin/users/users.scss`

**Bảng yêu cầu:**

| Username | Tên | Trạng thái | Role | Role Permission | Actions |
|----------|-----|-----------|------|-----------------|---------|
| admin | Nguyễn Văn A | 🟢 Active | `ADMIN_ALL` | `USER_READ, USER_UPDATE, ROLE_ALL, …` | Sửa · Role · Pass · Khóa · Xóa |
| staff01 | Trần Thị B | 🟡 Locked | `STAFF` | `TICKET_READ, TICKET_UPDATE` | … |

**Thay đổi cụ thể:**

1. **Cột "Tên"** — theo lựa chọn = `firstName + lastName` từ Keycloak:
   - Thêm field `firstName?: string`, `lastName?: string` vào `User` model (optional)
   - Trong bảng hiển thị: `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || user.username
   - Nếu BE chưa trả → fallback username
   - Sau này BE sync từ Keycloak sẽ tự hiển thị

2. **Cột "Trạng thái tài khoản"** — giữ logic `getStatus` hiện tại: `active` / `inactive` / `locked`

3. **Cột "Role"** — giữ nguyên (đã có)

4. **Cột "Role Permission" (mới)** — list tổng hợp permission từ tất cả role user:
   - Render `nz-tag` nhỏ, tối đa 5 cái
   - Nếu > 5 → tag cuối là `+N` mở `nz-modal` liệt kê đầy đủ
   - Aggregate `user.roles[].permissions[]`, dedupe theo `code`

5. **Actions mở rộng:**
   - **Sửa** (đã có `openEditModal`)
   - **Đổi role** (đã có `openRoleModal`) — bổ sung hiển thị permission sẽ add khi chọn role
   - **Reset mật khẩu** (đã có)
   - **Khóa/Mở khóa nhanh** (toggle `isLocked`) — mới
   - **Kích hoạt/Vô hiệu hóa** (toggle `isActive`) — đã có sẵn logic

**Route:** đã có `/admin/users` + `/admin/users/:id`, không cần thêm.

---

### A.2 — Roles Management (CRUD với mock cache)

**Files mới:**
- `layouts/admin/roles/roles.ts` + `.html` + `.scss`

**Bảng:**

| Code | Name | Description | Permissions | Active | Actions |
|------|------|-------------|-------------|--------|---------|
| ADMIN_ALL | Administrator | Toàn quyền hệ thống | 12 | ✓ | Sửa · Xóa |
| MANAGER | Manager | Quản lý khách sạn | 8 | ✓ | Sửa · Xóa |
| STAFF | Staff | Nhân viên CSKH | 5 | ✓ | Sửa · Xóa |
| USER | User | Khách hàng | 3 | ✓ | Sửa · Xóa |

**Modal Create/Edit Role:**
- Code (uppercase, unique, auto-format)
- Name, Description
- Multi-select permissions (12 perms hardcoded: `USER_READ/WRITE/DELETE`, `ROLE_*`, `BOOKING_*`, `TICKET_*`, `ROOM_*`, `HOTEL_*`, `PAYMENT_*`)
- Active toggle

**Modal Delete:** confirm + warning nếu role đang gán cho user

**Route mới (thêm vào `app.routes.ts`):**

```ts
{
  path: 'roles',
  canActivate: [roleGuard(['ADMIN_ALL'])],
  loadComponent: () => import('./layouts/admin/roles/roles').then(m => m.Roles)
}
```

---

### A.3 — Tickets (Admin xem tất cả)

**Hiện trạng:** `layouts/staff/staff-tickets/staff-tickets.ts` đã có. `TicketService.getAllTickets` đã gọi được `/api/admin/tickets`.

**Files:**
- **Mở rộng:** `staff-tickets.ts` thêm prop `mode: 'staff' | 'admin'`
- **Mới:** `layouts/admin/tickets/tickets.ts` (page riêng cho admin)
- **Mới:** `layouts/admin/tickets/ticket-detail.ts` (xem chi tiết)

**Bảng A.3:**

| ID | Title | Created By | Assigned To | Priority | Status | Created | Actions |
|----|-------|-----------|-------------|----------|--------|---------|---------|
| T-001 | Không đăng nhập được | user01 | staff01 | HIGH | IN_PROGRESS | 2026-06-20 | Xem · Assign · Đổi status |

**Filter:**
- Status (OPEN / IN_PROGRESS / RESOLVED / CLOSED)
- Priority (LOW / MEDIUM / HIGH / URGENT)
- AssignedTo (dropdown list staff)
- Date range
- Keyword search

**CRUD (tận dụng TicketService đã có):**
- List: `GET /api/admin/tickets` — `TicketService.getAllTickets()`
- Detail: `GET /api/tickets/:id` — `TicketService.getTicketById()`
- Assign: `PUT /api/admin/tickets/:id/assign?staffId=xxx` — `TicketService.assignTicket()`
- Update status: `PUT /api/admin/tickets/:id/status?status=…` — `TicketService.updateTicketStatus()`

**Routes mới:**

```ts
{
  path: 'tickets',
  canActivate: [roleGuard(['ADMIN_ALL'])],
  loadComponent: () => import('./layouts/admin/tickets/tickets').then(m => m.AdminTickets)
},
{
  path: 'tickets/:id',
  canActivate: [roleGuard(['ADMIN_ALL'])],
  loadComponent: () => import('./layouts/admin/tickets/ticket-detail').then(m => m.TicketDetail)
}
```

---

### A.4 — Bookings (Admin CRUD, mock cache)

**Hiện trạng:** `BookingService` đang mock localStorage. Tách riêng `AdminBookingService` cho admin dùng `MockCacheService`.

**Files mới:**
- `core/services/admin-booking.service.ts`
- `core/models/admin-booking.model.ts` (extend `Booking` thêm `userEmail`, `userPhone`)
- `layouts/admin/bookings/bookings.ts` + `.html`
- `layouts/admin/bookings/booking-detail.ts` + `.html`

**Bảng A.4:**

| ID | Khách hàng | Khách sạn | Phòng | Check-in | Check-out | Tổng tiền | Status | Payment | Actions |
|----|-----------|-----------|-------|----------|-----------|-----------|--------|---------|---------|
| BK-001 | Nguyễn Văn A | Vinpearl NT | Deluxe | 2026-07-01 | 2026-07-05 | 18,000,000đ | CONFIRMED | PAID | Xem · Hủy · Refund |

**Actions:**
- Xem chi tiết (mở page riêng hoặc drawer)
- Đổi status (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT → CANCELLED)
- Refund (toggle `paymentStatus`)
- Cancel + nhập lý do

**Route mới:**

```ts
{
  path: 'bookings',
  canActivate: [roleGuard(['ADMIN_ALL', 'MANAGER'])],
  loadComponent: () => import('./layouts/admin/bookings/bookings').then(m => m.AdminBookings)
},
{
  path: 'bookings/:id',
  canActivate: [roleGuard(['ADMIN_ALL', 'MANAGER'])],
  loadComponent: () => import('./layouts/admin/bookings/booking-detail').then(m => m.AdminBookingDetail)
}
```

---

### A.5 — Rooms (flat table, CRUD với mock cache)

**Files mới:**
- `core/services/rooms.service.ts` (dùng `MockCacheService`)
- `core/models/room-admin.model.ts`
- `layouts/admin/rooms/rooms.ts` + `.html`

**Bảng A.5 (flat):**

| ID | Tên phòng | Khách sạn | Loại | Giá/đêm | Sức chứa | Số phòng còn | Trạng thái | Actions |
|----|-----------|-----------|------|---------|---------|------------|-----------|---------|
| R-001 | Deluxe Ocean View | Vinpearl NT | DELUXE | 4,500,000đ | 2 người | 5 | Active | Sửa · Xóa |

**Modal Create/Edit Room:**
- Tên phòng, Khách sạn (dropdown), Loại phòng (dropdown)
- Giá/đêm (number + currency VND), Giá gốc (optional để tính discount)
- Sức chứa (maxAdults, maxChildren), bedType, size (m²)
- Số phòng còn (available), Trạng thái active
- **Upload ảnh** (dùng `nz-upload` + `FileReader` → base64 lưu cache, không cần BE)
- Amenities (multi-select checkbox)
- Chính sách: freeCancellation, breakfastIncluded, payLater

**Route mới:**

```ts
{
  path: 'rooms',
  canActivate: [roleGuard(['ADMIN_ALL', 'MANAGER'])],
  loadComponent: () => import('./layouts/admin/rooms/rooms').then(m => m.Rooms)
}
```

---

### A.6 — Hotel Rooms (master-detail, dùng chung data A.5)

**Files mới:**
- `core/services/hotels.service.ts` (dùng `MockCacheService` seed từ `hotel.model.ts` hiện có)
- `layouts/admin/hotel-rooms/hotel-rooms.ts` + `.html`
- `layouts/admin/hotel-rooms/hotel-rooms-detail.ts` (xem chi tiết 1 hotel + danh sách phòng)

**Layout:** Master-detail:
- Cột trái: danh sách hotel (card/list) với thumbnail, tên, thành phố, số phòng
- Cột phải: chọn 1 hotel → hiện grid các phòng của hotel đó (dùng chung `RoomsService`)
- Nút "Thêm khách sạn", "Sửa" trên từng card

**Modal Create/Edit Hotel:**
- Tên, tên EN, mô tả
- Địa chỉ, thành phố, quốc gia
- Tọa độ (lat/lng), quận/huyện
- Số sao (1-5)
- Amenities (multi-select)
- **Upload ảnh** (nhiều ảnh)
- Chính sách: checkIn, checkOut, cancellation, children, pets, smoking

**Route mới:**

```ts
{
  path: 'hotel-rooms',
  canActivate: [roleGuard(['ADMIN_ALL', 'MANAGER'])],
  loadComponent: () => import('./layouts/admin/hotel-rooms/hotel-rooms').then(m => m.HotelRooms)
},
{
  path: 'hotel-rooms/:id',
  canActivate: [roleGuard(['ADMIN_ALL', 'MANAGER'])],
  loadComponent: () => import('./layouts/admin/hotel-rooms/hotel-rooms-detail').then(m => m.HotelRoomsDetail)
}
```

---

## 🟢 TỔNG KẾT PART A

| # | Task | File mới | File sửa | Route mới |
|---|------|---------|---------|-----------|
| A.1 | Account + cột Permission | – | `users.ts/html/scss` | – |
| A.2 | Roles CRUD mock | `roles/*`, `mock-cache.service.ts` | – | `/admin/roles` |
| A.3 | Tickets Admin | `tickets.ts`, `ticket-detail.ts` | mở rộng `staff-tickets.ts` | `/admin/tickets`, `/admin/tickets/:id` |
| A.4 | Bookings Admin mock | `bookings/*`, `admin-booking.service.ts` | – | `/admin/bookings`, `/admin/bookings/:id` |
| A.5 | Rooms flat mock | `rooms/*`, `rooms.service.ts` | – | `/admin/rooms` |
| A.6 | Hotel Rooms mock | `hotel-rooms/*`, `hotels.service.ts` | – | `/admin/hotel-rooms`, `/admin/hotel-rooms/:id` |

**Core service chung cần tạo 1 lần:** `core/services/mock-cache.service.ts` (in-memory + sessionStorage + BehaviorSubject)

**Thứ tự code đề xuất:**
1. `MockCacheService` (foundation cho A.2, A.4, A.5, A.6)
2. A.1 (sửa users.html) — độc lập, dùng BE thật
3. A.2 (roles) — seed **3 role: ADMIN, HOST, USER** (xem chi tiết bên dưới) + permission theo bảng phân quyền mới
4. A.3 (tickets) — dùng API thật
5. A.4 (bookings) — dùng cache + manual workflow (admin/host đổi status thủ công)
6. A.5 (rooms) — dùng cache
7. A.6 (hotel-rooms) — dùng cache + A.5
8. A.0 (sidebar) — thêm link + điều kiện hiển thị theo role cuối cùng

---

## 🔐 BẢNG PHÂN QUYỀN MỚI (3 ROLE)

> Thay thế 4 role cũ (ADMIN_ALL/MANAGER/STAFF/USER) bằng 3 role mới (ADMIN/HOST/USER).
> Logic: Sàn trung gian — host cho thuê phòng, user đặt phòng, admin quản lý tất cả.

| Module / Page | ADMIN | HOST | USER |
|---------------|:-----:|:----:|:----:|
| **Tổng quan** | | | |
| Dashboard (global) | ✅ | ⚠️ scoped | ❌ |
| Dashboard — biểu đồ doanh thu | ✅ all hotels | ✅ only own hotels | ❌ |
| **Quản lý tài khoản** | | | |
| Tài khoản (`/admin/users`) | ✅ | ❌ | ❌ |
| Phân quyền (`/admin/roles`) | ✅ | ❌ | ❌ |
| **Quản lý khách sạn** | | | |
| Khách sạn (`/admin/hotels`) | ✅ all | ✅ own (CRUD) | ❌ |
| Phòng khách sạn (`/admin/hotel-rooms`) | ✅ all | ✅ own (CRUD) | ❌ |
| Loại phòng (`/admin/room-types`) | ✅ all | ✅ own | ❌ |
| Phòng (`/admin/rooms`) | ✅ all | ✅ own (CRUD) | ❌ |
| **Đặt phòng** | | | |
| Đơn đặt phòng (`/admin/bookings`) | ✅ all | ⚠️ own hotels only | ❌ |
| Thanh toán (`/admin/payments`) | ✅ all | ⚠️ own hotels only | ❌ |
| **Hỗ trợ** | | | |
| Phiếu hỗ trợ (`/admin/tickets`) | ✅ all | ✅ own assigned | ✅ own created |
| **User pages** (`/user/*`) | | | |
| My bookings, profile, my tickets | ✅ | ✅ | ✅ |

### Permission matrix chi tiết

| Permission | ADMIN | HOST | USER |
|-----------|:-----:|:----:|:----:|
| `USER_READ/WRITE/DELETE` | ✅ | ❌ | ❌ |
| `ROLE_READ/WRITE/DELETE` | ✅ | ❌ | ❌ |
| `HOTEL_READ_ALL` | ✅ | ❌ | ❌ |
| `HOTEL_READ_OWN` | ✅ | ✅ | ❌ |
| `HOTEL_WRITE_ALL` | ✅ | ❌ | ❌ |
| `HOTEL_WRITE_OWN` | ✅ | ✅ | ❌ |
| `ROOM_READ_ALL` | ✅ | ❌ | ❌ |
| `ROOM_READ_OWN` | ✅ | ✅ | ❌ |
| `ROOM_WRITE_ALL` | ✅ | ❌ | ❌ |
| `ROOM_WRITE_OWN` | ✅ | ✅ | ❌ |
| `ROOM_TYPE_READ_ALL` | ✅ | ❌ | ❌ |
| `ROOM_TYPE_READ_OWN` | ✅ | ✅ | ❌ |
| `ROOM_TYPE_WRITE_ALL` | ✅ | ❌ | ❌ |
| `ROOM_TYPE_WRITE_OWN` | ✅ | ✅ | ❌ |
| `BOOKING_READ_ALL` | ✅ | ❌ | ❌ |
| `BOOKING_READ_OWN_HOTEL` | ✅ | ✅ | ❌ |
| `BOOKING_WRITE_ALL` | ✅ | ❌ | ❌ |
| `BOOKING_WRITE_OWN_HOTEL` | ✅ | ✅ | ❌ |
| `BOOKING_CONFIRM_OWN_HOTEL` | ✅ | ✅ | ❌ |
| `PAYMENT_READ_ALL` | ✅ | ❌ | ❌ |
| `PAYMENT_READ_OWN_HOTEL` | ✅ | ✅ | ❌ |
| `PAYMENT_CONFIRM_OWN_HOTEL` | ✅ | ✅ | ❌ |
| `TICKET_READ_ALL` | ✅ | ❌ | ❌ |
| `TICKET_READ_OWN` | ✅ | ✅ | ✅ |
| `TICKET_WRITE_ALL` | ✅ | ❌ | ❌ |
| `TICKET_WRITE_OWN` | ✅ | ✅ | ✅ |
| `DASHBOARD_VIEW_ALL` | ✅ | ❌ | ❌ |
| `DASHBOARD_VIEW_OWN_HOTEL` | ✅ | ✅ | ❌ |

### Tổng số permission
- ADMIN: ~30 (toàn quyền)
- HOST: ~13 (scoped theo hotels của mình)
- USER: ~2 (chỉ ticket cá nhân + quyền mặc định ở /user/*)

### Scope của HOST
- HOST là chủ khách sạn cho thuê
- Khi tạo hotel → gắn `hotel.ownerId = currentUser.id`
- Mọi query của HOST đều filter theo `ownerId`
- BE phải check ownership trước khi cho phép CRUD
- Vì mock cache → FE phải tự filter (đánh dấu `currentUserId` lúc seed data)

---

## 📊 DASHBOARD — Biểu đồ & Trạng thái

### Dashboard layout (admin layout dùng chung cho admin + host)

**KPI cards (hàng trên, 4 thẻ):**
- Tổng doanh thu (30 ngày) — `finalPrice` cộng dồn của booking PAID trong khoảng
- Tổng đặt phòng — count booking
- Tỷ lệ lấp đầy — % phòng có booking / tổng phòng
- Đánh giá TB — average rating (nếu có)

**Biểu đồ doanh thu (chart lớn, 2/3 width):**
- Chart.js line/area chart
- Trục X: 30 ngày gần nhất
- Trục Y: doanh thu theo ngày (VND)
- ADMIN: tổng hợp tất cả hotels
- HOST: chỉ hotels của mình
- Tooltip hiện giá trị + số booking trong ngày

**Biểu đồ tròn trạng thái đặt phòng (1/3 width):**
- Chart.js doughnut chart, 6 status: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW
- ADMIN: tất cả bookings · HOST: bookings của hotels mình

**Bảng "Đơn đặt phòng gần đây" (dưới chart):**
- 10 dòng mới nhất
- Columns: ID, Khách hàng, Hotel, Phòng, Check-in, Tổng tiền, Trạng thái, Thanh toán

**Bảng "Thanh toán cần xử lý" (nếu là admin/host):**
- Filter `paymentStatus = UNPAID` hoặc `paymentStatus = PAID && booking.status = PENDING (chờ host confirm)`
- Columns: ID booking, Khách hàng, Hotel, Số tiền, Trạng thái TT, Trạng thái đặt, Action (Confirm/Cancel)
- Nút **"Xác nhận phòng"** chỉ hiện với HOST (confirm booking của hotel mình) hoặc ADMIN
- Nút **"Đánh dấu đã thanh toán"** — admin thấy tất cả, host thấy của hotel mình

### Trang Thanh toán (`/admin/payments`)

**Layout 3 tab (nz-tabs):**

**Tab 1 — Chưa thanh toán (`UNPAID`):**
- Bảng các booking có `paymentStatus = UNPAID`
- Cột: ID, Khách hàng, Hotel, Phòng, Check-in, Số tiền, Trạng thái đặt, Action
- Action: **"Xác nhận đã nhận tiền"** (chỉ admin/host của hotel đó)

**Tab 2 — Đã thanh toán (`PAID`):**
- Bảng các booking đã thanh toán
- Action: **"Hoàn tiền"** (chỉ admin)

**Tab 3 — Chờ xác nhận phòng (booking status PENDING + PAID):**
- Bảng các booking đã trả tiền nhưng host chưa xác nhận phòng
- Cột: ID, Khách hàng, Hotel, Phòng, Check-in, Số tiền, Action
- Action: **"Xác nhận phòng"** (host của hotel đó / admin)

### File cần tạo/sửa cho dashboard

**Mở rộng dashboard hiện tại:**
- `layouts/admin/dashboard/dashboard.ts` + `.html` + `.scss`
- Thêm `revenue-chart.component` (Chart.js line chart)
- Thêm `booking-status-chart.component` (Chart.js doughnut chart)
- Thêm `recent-bookings-table.component`
- Thêm `pending-payments-table.component`

**Service:**
- Mở rộng `core/services/dashboard.ts` — thêm method `getRevenueStats(range, scope)` và `getBookingStatusStats(scope)`
- Vì mock → `AdminBookingService` aggregate dữ liệu cache theo scope (`'all' | 'own'`)

---

## 🔗 LIÊN KẾT TÀI KHOẢN (Account Linking)

### Câu hỏi: User login bằng username → sau đó nhập email → email đó có dùng để link với Keycloak không?

**Trả lời: CẦN CÓ MỤC LIÊN KẾT TÀI KHOẢN.** Lý do:

### Tại sao cần Account Linking?

**Cơ chế Identity Provider:**
- Keycloak dùng **email** làm identifier chính khi liên kết với external IdP (Google, Facebook, GitHub...)
- Email = "primary key" trong OAuth/OIDC world
- Username chỉ là local identifier trong từng realm

**3 trường hợp user:**

| Trường hợp | Login | Email đã có trong KC? | Cần link? |
|------------|-------|----------------------|-----------|
| 1. User đăng ký qua form Angular (username) | username | ❌ Chưa | Sau khi nhập email → link |
| 2. User đăng ký qua Keycloak UI | username hoặc email | ✅ Có | ❌ đã link sẵn |
| 3. User login bằng Google | email | ✅ Có | ❌ link tự động |

**Trường hợp 1** là phổ biến nhất trong giai đoạn đầu: user đăng ký form FE bằng username → sau đó vào profile thêm email → **email đó cần được link với Keycloak user account** để sau này có thể login qua Google.

### Mục "Liên kết tài khoản" trong Profile

**Vị trí:** `/user/profile` — tab **"Tài khoản & Bảo mật"** hoặc section riêng.

**UI:**

```
┌─────────────────────────────────────────────────┐
│ 🔗 Tài khoản liên kết                           │
├─────────────────────────────────────────────────┤
│ SmartBooking:                                   │
│   ✅ Đã liên kết (username: nguyenvana)         │
│   [Đổi mật khẩu]                               │
│                                                 │
│ Keycloak (SSO):                                 │
│   ⚠️ Email chưa xác thực                       │
│   [Xác thực email ngay]                        │
│                                                 │
│ Google:                                         │
│   ❌ Chưa liên kết                              │
│   [Liên kết với Google]                        │
│                                                 │
│ Facebook:                                       │
│   ❌ Chưa liên kết                              │
│   [Liên kết với Facebook]                      │
└─────────────────────────────────────────────────┘
```

**Flow xác thực email để link Keycloak:**

1. User vào profile → thấy "Email chưa xác thực"
2. Bấm **"Xác thực email ngay"** → gửi OTP 6 số về email
3. User nhập OTP → FE gọi `POST /api/users/me/verify-email` → BE sync email lên Keycloak user
4. Email được verify → status đổi thành ✅ "Đã xác thực"
5. Từ đó có thể login qua Keycloak bằng email, hoặc link Google sau này

**Flow liên kết Google (tương lai):**

1. User bấm **"Liên kết với Google"**
2. Redirect sang Google OAuth consent screen
3. Google redirect về `http://localhost:8180/realms/booking/broker/google/link`
4. Keycloak xử lý broker linking → set session binding
5. Redirect về FE callback → success

### File cần tạo/sửa cho Account Linking

- **Mở rộng:** `views/user/profile/profile.component.ts` + `.html` — thêm tab "Tài khoản & Bảo mật"
- **Mới:** `core/services/account-linking.service.ts` — gọi `/api/users/me/verify-email`, `/api/users/me/link-provider`
- **Mới:** `core/models/account-linking.model.ts` — `LinkedProvider { type: 'smartbooking'|'keycloak'|'google'|'facebook', verified: boolean, linkedAt: string }`

### Đồng bộ Keycloak

**Trong `docker/keycloak/realms/booking-realm.json`** — bật sẵn:

```json
{
  "identityProviders": [
    {
      "alias": "google",
      "providerId": "google",
      "enabled": false,
      "config": {
        "clientId": "TO_BE_CONFIGURED",
        "clientSecret": "TO_BE_CONFIGURED"
      }
    }
  ],
  "link": "smartbooking-fe",
  "editUsernameAllowed": false
}
```

**`editUsernameAllowed: false`** — quan trọng: sau khi link với Keycloak, user không thể tự ý đổi username (vì username trở thành identity key).

### Scope của Plan

> **Trong phạm vi Part A+B lần này:** chỉ làm **UI mục Liên kết tài khoản** + **method verify email**. Phần thật sự link Google/Facebook là tương lai — chỉ để UI sẵn.

---

## 🅱️ PART B — HƯỚNG DẪN CONFIG KEYCLOAK MOCK LOGIN/REGISTER

---

## 🅱️ PART B — HƯỚNG DẪN CONFIG KEYCLOAK MOCK LOGIN/REGISTER

> **Lựa chọn của user:** "Cả 2, có toggle 'Đăng nhập nhanh'" — Form Angular giữ nguyên + nút "Đăng nhập qua Keycloak" redirect sang `localhost:8180`.

### B.1 — Hiểu flow hiện tại

**Stack Keycloak đang có:**
- Container: `quay.io/keycloak/keycloak:24.0.3`, port 8180
- Realm: `booking` (file `docker/keycloak/realms/booking-realm.json`)
- Client: `booking-frontend` (public, redirect URI `http://localhost:4200/*`)
- Admin console: `http://localhost:8180/admin` (user `admin`, pass `Admin@2024`)
- Login page mặc định: `http://localhost:8180/realms/booking/protocol/openid-connect/auth?...`

**Code Angular đang có:**
- `core/services/keycloak.service.ts` — đã có sẵn `getAuthorizationUrl()` cho PKCE flow
- `views/auth/login/login.component.html` — form login Angular
- `views/auth/register/register.component.html` — form register Angular
- `views/auth/callback/callback.component.ts` — nhận `code` từ Keycloak redirect về

→ **Flow Keycloak đã được wire sẵn 90%.** Chỉ cần thêm "toggle" + fix một số thứ trong realm config.

### B.2 — Checklist config Keycloak

Bạn mở `http://localhost:8180/admin` → login `admin / Admin@2024` → chọn realm `booking` → làm theo:

**Bước 1: Bật đăng ký (Register)**
- Realm settings → tab **Login** → bật **User registration** = ON
- Cho phép user tự tạo tài khoản qua Keycloak UI

**Bước 2: Bật Forgot password**
- Cùng tab **Login** → bật **Forgot password** = ON
- User có thể bấm "Forgot password" trên trang Keycloak

**Bước 3: Email settings (để forgot password hoạt động)**
- Realm settings → tab **Email** → điền SMTP (dev có thể dùng MailHog hoặc để trống → keycloak chỉ log ra console)
- Với dev: **Test mode** → click "Test connection" không cần thiết, chỉ cần điền From + host dummy
- Production sau này đổi SMTP thật

**Bước 4: Theme (tuỳ chọn, đẹp hơn)**
- Realm settings → tab **Themes**:
  - Login theme: `keycloak.v3` (mặc định) hoặc import custom theme sau
  - Tạm thời để mặc định

**Bước 5: Client `booking-frontend`**
- Clients → `booking-frontend` → tab **Settings**:
  - **Valid redirect URIs**: `http://localhost:4200/*`
  - **Web origins**: `http://localhost:4200` (không có dấu `/*`)
  - **PKCE Code Challenge Method**: `S256` (đã có sẵn trong code)
  - **Advanced → OAuth 2.0 Compatibility**: BẮT BUỘC tắt **"Require Proof Key for Code Exchange (PKCE)"** = ON (đã đúng rồi)

**Bước 6: Tạo role đầy đủ (nếu chưa có)**
- Realm roles → kiểm tra có 4 role: `ADMIN`, `MANAGER`, `STAFF`, `USER`
- Nếu thiếu → Create role: name + description

**Bước 7: Tạo user test**
- Users → Add user:
  - Username: `admin`
  - Email: `admin@test.com`
  - First name: `Nguyễn Văn`
  - Last name: `A`
  - Email verified: ON
- Tab **Credentials** → Set password: `Admin@123` → tạm tắt "Temporary"
- Tab **Role mapping** → Assign role `ADMIN`

### B.3 — Frontend changes (toggle)

**File sửa: `views/auth/login/login.component.html`**

Thêm block trên cùng form:

```html
<div class="login-methods">
  <button nz-button nzType="primary" nzSize="large" nzBlock
          (click)="loginWithKeycloak()" class="kc-btn">
    <span nz-icon nzType="key"></span>
    Đăng nhập nhanh qua Keycloak (mock)
  </button>

  <nz-divider nzText="HOẶC"></nz-divider>
</div>
```

**File sửa: `views/auth/login/login.component.ts`**

```ts
async loginWithKeycloak() {
  await this.auth.loginWithKeycloak(); // đã có sẵn trong AuthService
}
```

**File sửa: `views/auth/register/register.component.html`**

Tương tự — thêm nút "Đăng ký nhanh qua Keycloak" → `window.location.href = 'http://localhost:8180/realms/booking/protocol/openid-connect/registrations?client_id=booking-frontend&response_type=code&redirect_uri=http://localhost:4200/auth/callback&code_challenge=...&code_challenge_method=S256'`.

**Callback handler đã có sẵn** (`views/auth/callback/callback.component.ts`) — nhận `code` → gọi `AuthService.exchangeCode(code, verifier, redirectUri)` → BE đổi lấy JWT + set HttpOnly cookie. **Không cần sửa.**

### B.4 — Test flow

1. Start docker compose: `docker compose up -d`
2. Chờ keycloak ready (check `docker logs booking-keycloak`)
3. Start frontend: `cd frontend/booking-admin-ui && npm start`
4. Mở `http://localhost:4200/auth/login`
5. Bấm **"Đăng nhập nhanh qua Keycloak"**
6. Redirect sang `http://localhost:8180/realms/booking/login` → trang đẹp của Keycloak
7. Đăng nhập `admin / Admin@123`
8. Redirect về `http://localhost:4200/auth/callback?code=...`
9. Callback component gọi BE exchange → set cookie → navigate về `/admin/dashboard`

### B.5 — Lưu ý quan trọng

- **Form Angular giữ nguyên 100%** — chỉ thêm 1 nút. User có thể chọn login bằng form thường (gọi `/api/auth/login` của auth-service) HOẶC login qua Keycloak.
- **`/api/auth/register` của auth-service** vẫn hoạt động song song với Keycloak registration. Khi BE sync user từ Keycloak về DB thì 2 nguồn sẽ converge.
- **Theme custom nhưng fallback an toàn:** Login theme trong realm settings đặt thành custom theme (file trong `docker/keycloak/themes/smartbooking/`). **Nếu custom theme lỗi** (CSS hỏng, template sai) → vào admin console → Realm settings → Themes → Login theme → chọn `keycloak.v3` (mặc định) → Save. Không cần restart container. **KHÔNG xóa file theme**, chỉ switch theme setting.
- **Identity provider Google:** Plan đã note trong `booking-realm.json` (alias `google`, `enabled: false`). Để bật sau này: tạo OAuth client trên Google Console → lấy clientId/clientSecret → vào Keycloak admin → Identity providers → Google → paste config → enable. Không cần đổi code FE (đã dùng Account Linking UI).
- **Account Linking:** Section "Liên kết tài khoản" trong `/user/profile` đã được thêm vào scope của plan (xem mục 🔗 LIÊN KẾT TÀI KHOẢN ở trên).
- **Nếu Keycloak container chưa import realm** (do `--import-realm` không hoạt động), vào admin console → Create realm → import file `docker/keycloak/realms/booking-realm.json`.

---

## ✅ CHECKLIST TỔNG

| # | Task | Loại | Phụ thuộc |
|---|------|------|-----------|
| 0 | `MockCacheService` foundation | Tạo mới | – |
| 1 | A.1 Account | Sửa | – |
| 2 | A.2 Roles CRUD mock (seed 3 role: ADMIN/HOST/USER) | Tạo mới | #0 |
| 3 | A.3 Tickets Admin | Tạo mới | – |
| 4 | A.4 Bookings Admin mock + manual workflow | Tạo mới | #0 |
| 5 | A.5 Rooms flat mock | Tạo mới | #0 |
| 6 | A.6 Hotel Rooms mock | Tạo mới | #0, #5 |
| 7 | A.0 Sidebar update + điều kiện hiển thị theo role | Sửa | #1-#6 |
| 8 | Dashboard mở rộng (chart doanh thu, trạng thái) | Sửa | #4 |
| 9 | Thanh toán 3 tab (UNPAID/PAID/PENDING-CONFIRM) | Tạo mới | #4 |
| 10 | Account Linking UI trong profile | Sửa | – |
| ── | **── Hết Part A, chuyển sang Part B ──** | | |
| 11 | B.1-B.7 Keycloak config | Config thủ công | Keycloak running |
| 12 | B.3 FE toggle "Đăng nhập nhanh" | Sửa | #11 |

---

## ❓ CÂU HỎI CÒN LẠI (cần bạn xác nhận trước khi code)

1. **A.1 Tên hiển thị** — Khi BE chưa trả `firstName/lastName`, fallback = username. Bạn OK chứ?
2. **A.2 Roles mock** — ~30 permission cho ADMIN, ~13 cho HOST, ~2 cho USER. Đủ chưa?
3. **A.4 Bookings manual workflow** — admin/host đổi status thủ công (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT → CANCELLED). Không tự động. OK chứ?
4. **A.5/A.6 ảnh phòng** — Upload base64 lưu sessionStorage (giới hạn ~5-10MB). OK hay cần mock URL khác?
5. **HOST scope FE** — Vì mock cache, FE tự filter theo `currentUserId`. Cần trang `/admin/hotels` có bộ lọc "Hotel của tôi" / "Tất cả". OK chứ?
6. **Dashboard HOST** — Chart doanh thu chỉ hiện hotels của host. Có cần so sánh với TB hệ thống (benchmark) không?
7. **Account Linking** — Làm UI + verify email. Phần link Google thật sự để sau. OK chứ?
8. **Thứ tự Part A** — Checklist trên đã hợp lý chưa? (đề xuất: 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10, sau đó mới chuyển Part B)
