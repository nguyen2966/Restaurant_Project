# TEST PLAN — Hệ Thống Quản Lý Nhà Hàng

> **Phiên bản:** 1.1 | **Ngày:** 2026-04 | **Môi trường:** `http://localhost:8080`
> **Công cụ:** Swagger UI (`/swagger-ui/index.html`) hoặc Postman

---

## Mục Lục

1. [Quy ước & Chuẩn bị](#1-quy-ước--chuẩn-bị)
2. [Module AUTH — Xác thực](#2-module-auth--xác-thực)
3. [Module MENU — Thực đơn](#3-module-menu--thực-đơn)
4. [Module PROMOTION — Khuyến mãi](#4-module-promotion--khuyến-mãi)
5. [Module SEATING — Bàn ghế & Đặt bàn](#5-module-seating--bàn-ghế--đặt-bàn)
6. [Module ORDER — Tạo đơn hàng](#6-module-order--tạo-đơn-hàng)
7. [Module KITCHEN — Nhà bếp & Station](#7-module-kitchen--nhà-bếp--station)
8. [Module INVENTORY — Kho nguyên liệu](#8-module-inventory--kho-nguyên-liệu)
9. [Module PAYMENT — Thanh toán](#9-module-payment--thanh-toán)
10. [Module REPORT — Báo cáo](#10-module-report--báo-cáo)
11. [Kiểm thử Bảo mật & Phân quyền](#11-kiểm-thử-bảo-mật--phân-quyền)
12. [Kiểm thử Luồng End-to-End](#12-kiểm-thử-luồng-end-to-end)
13. [Kiểm thử Nghiệp vụ Đặc biệt](#13-kiểm-thử-nghiệp-vụ-đặc-biệt)

---

## 1. Quy ước & Chuẩn bị

### 1.1 Ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| ✅ | Kết quả mong đợi — PASS |
| ❌ | Kết quả mong đợi — FAIL (hệ thống phải trả lỗi) |
| `{id}` | Thay bằng ID thực tế lấy từ response trước |
| `[MANAGER]` | Cần token của role tương ứng |
| 💾 | Lưu giá trị này để dùng cho các test case sau |

### 1.2 Format Response chuẩn

Mọi response đều có dạng:
```json
{
  "success": true | false,
  "data": { ... } | null,
  "message": "..." | null,
  "timestamp": "2026-04-01T10:30:00"
}
```

**Lỗi 401 (chưa xác thực):**
```json
{
  "success": false,
  "data": null,
  "message": "Unauthenticated: ...",
  "timestamp": "2026-04-01T10:30:00"
}
```

**Lỗi 403 (sai role):**
```json
{
  "success": false,
  "data": null,
  "message": "Access denied: you do not have the required role for this action.",
  "timestamp": "2026-04-01T10:30:00"
}
```

### 1.3 Tài khoản mock (tất cả dùng password `123456`)

> Tất cả tài khoản bên dưới là **mock data** dùng cho mục đích kiểm thử.
> Password thống nhất: **`123456`**

| Username | Password | Role | Mô tả |
|----------|----------|------|-------|
| `admin` | `123456` | MANAGER | Toàn quyền hệ thống |
| `server_1` | `123456` | SERVER | Nhân viên phục vụ |
| `chef_1` | `123456` | CHEF | Đầu bếp |
| `cashier_1` | `123456` | CASHIER | Thu ngân |

### 1.4 Thứ tự thực hiện

> **Quan trọng:** Các test case có đánh số phải chạy **tuần tự** vì dữ liệu phụ thuộc nhau.
> Chạy từ Section 2 → 12 để cover toàn bộ luồng.

---

## 2. Module AUTH — Xác thực

### TC-AUTH-01 ✅ Đăng nhập thành công với MANAGER

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.token` là chuỗi JWT (bắt đầu bằng `eyJ`)
- `data.role` = `"MANAGER"`
- `data.expiresInSeconds` = `86400`

> 💾 Lưu: `TOKEN_MANAGER = data.token`

---

### TC-AUTH-02 ✅ Đăng nhập với các role khác

```json
{ "username": "server_1",  "password": "123456" }  // → 💾 TOKEN_SERVER
{ "username": "chef_1",    "password": "123456" }  // → 💾 TOKEN_CHEF
{ "username": "cashier_1", "password": "123456" }  // → 💾 TOKEN_CASHIER
```

**Kết quả mong đợi:** HTTP `200`, mỗi token có `role` đúng tương ứng.

---

### TC-AUTH-03 ❌ Đăng nhập sai mật khẩu

```json
{ "username": "admin", "password": "wrongpassword" }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa thông báo sai thông tin đăng nhập

---

### TC-AUTH-04 ❌ Gọi API không có token

```
GET /api/menu
(không có header Authorization)
```

**Kết quả mong đợi:**
- HTTP `401 Unauthorized`
- Response trả về **JSON** đúng format (không phải trang HTML Tomcat)
- `message` chứa `"Unauthenticated"`

---

### TC-AUTH-05 ✅ Validate token còn hiệu lực

```
POST /api/auth/validate
Authorization: Bearer {TOKEN_MANAGER}
```

**Kết quả mong đợi:**
- `data: true`
- `message: "Token còn hiệu lực"`

---

## 3. Module MENU — Thực đơn

### TC-MENU-01 ✅ Xem toàn bộ menu

```
GET /api/menu
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array các item có: `id`, `name`, `basePrice`, `status`

---

### TC-MENU-02 ✅ Lọc theo status ACTIVE

```
GET /api/menu?status=ACTIVE
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** Tất cả items `status: "ACTIVE"`

---

### TC-MENU-03 ✅ Tìm kiếm theo tên (case-insensitive)

```
GET /api/menu?query=phở
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** Kết quả chứa các món liên quan đến "phở"

---

### TC-MENU-04 ✅ Tạo món mới

```
POST /api/menu
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Bún Chả Hà Nội",
  "basePrice": 75000,
  "description": "Bún chả đặc trưng với nước chấm chuẩn vị",
  "allergens": ["gluten"]
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.status: "ACTIVE"`

> 💾 Lưu: `MENU_ITEM_ID_NEW = data.id`

---

### TC-MENU-05 ✅ Cập nhật thông tin món

```
PUT /api/menu/{MENU_ITEM_ID_NEW}
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Bún Chả Hà Nội Đặc Biệt",
  "basePrice": 85000,
  "description": "Thêm chả cốm",
  "allergens": ["gluten", "egg"]
}
```

**Kết quả mong đợi:** `data.basePrice: 85000`

---

### TC-MENU-06 ✅ Đổi trạng thái sang OUT_OF_STOCK rồi ACTIVE lại

```
PATCH /api/menu/{MENU_ITEM_ID_NEW}/status
{ "status": "OUT_OF_STOCK" }
// → data.status: "OUT_OF_STOCK"

PATCH /api/menu/{MENU_ITEM_ID_NEW}/status
{ "status": "ACTIVE" }
// → data.status: "ACTIVE"
```

---

### TC-MENU-07 ❌ Tạo món thiếu basePrice

```
POST /api/menu
Authorization: Bearer {TOKEN_MANAGER}
{ "name": "Thiếu giá" }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa `"Base price is required"`

---

### TC-MENU-08 ❌ SERVER không thể tạo món

```
POST /api/menu
Authorization: Bearer {TOKEN_SERVER}
{ "name": "Test", "basePrice": 50000 }
```

**Kết quả mong đợi:**
- HTTP `403 Forbidden`

---

## 4. Module PROMOTION — Khuyến mãi

### TC-PROMO-01 ✅ Tạo khuyến mãi BY_PERCENT

```
POST /api/promotions
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Giảm 20% Drinks Happy Hour",
  "promoType": "BY_PERCENT",
  "condition": "Áp dụng 14:00-17:00",
  "menuItemIds": [7, 8, 9],
  "startDate": "2026-04-01T00:00:00",
  "dueDate": "2026-04-30T23:59:59",
  "discountValue": 20
}
```

> 💾 Lưu: `PROMO_ID_PERCENT = data.id`

---

### TC-PROMO-02 ✅ Simulate discount

```
POST /api/promotions/simulate
Authorization: Bearer {TOKEN_MANAGER}

{
  "menuItemQuantities": { "7": 2, "8": 1 },
  "subTotal": 125000
}
```

**Kết quả mong đợi:**
- `data.discountAmount` = `25000` (20% × 125,000)
- `data.finalTotal` = `100000`

---

### TC-PROMO-03 ❌ Disable menu item đang trong active promo

```
DELETE /api/menu/7
Authorization: Bearer {TOKEN_MANAGER}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"referenced by an active promotion"`

---

### TC-PROMO-04 ✅ Đổi promo sang INACTIVE (để test disable item sau)

```
PATCH /api/promotions/{PROMO_ID_PERCENT}/status
{ "status": "INACTIVE" }
```

**Kết quả mong đợi:** `data.status: "INACTIVE"`

---

## 5. Module SEATING — Bàn ghế & Đặt bàn

### TC-SEAT-01 ✅ Xem sơ đồ bàn

```
GET /api/seating/tables
Authorization: Bearer {TOKEN_SERVER}
```

> 💾 Lưu: `TABLE_ID_AVAILABLE` = id một bàn AVAILABLE

---

### TC-SEAT-02 ✅ Nhận khách walk-in

```
POST /api/seating/tables/{TABLE_ID_AVAILABLE}/seat-walkin?partySize=2
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** `data.status: "SEATED"`

> 💾 Lưu: `TABLE_ID_SEATED = TABLE_ID_AVAILABLE`

---

### TC-SEAT-03 ❌ Walk-in vào bàn đã có khách

```
POST /api/seating/tables/{TABLE_ID_SEATED}/seat-walkin?partySize=2
```

**Kết quả mong đợi:** HTTP `409 Conflict`

---

### TC-SEAT-04 ✅ Đặt bàn trước

```
POST /api/seating/reservations
Authorization: Bearer {TOKEN_SERVER}

{
  "customerName": "Trần Thị Bình",
  "customerPhone": "0987654321",
  "partySize": 4,
  "reservedTime": "2026-04-10T19:00:00"
}
```

> 💾 Lưu: `RESERVATION_ID = data.id`

---

### TC-SEAT-05 ✅ Thêm vào waitlist

```
POST /api/seating/waitlist
Authorization: Bearer {TOKEN_SERVER}

{ "customerName": "Lê Văn Cường", "partySize": 3 }
```

**Kết quả mong đợi:** `data.isNotified: false`

---

## 6. Module ORDER — Tạo đơn hàng

### TC-ORDER-01 ✅ Tạo order DINE_IN thành công

```
POST /api/orders
Authorization: Bearer {TOKEN_SERVER}
X-User-Id: 2

{
  "tableId": {TABLE_ID_SEATED},
  "type": "DINE_IN",
  "items": [
    { "menuItemId": 1, "quantity": 2, "specialNotes": "Ít hành", "options": {} },
    { "menuItemId": 8, "quantity": 2, "specialNotes": null, "options": {} }
  ]
}
```

**Kết quả mong đợi:**
- `data.status: "PLACED"`
- `data.subTotal` = `(75000 × 2) + (35000 × 2)` = `220000`
- `data.items[0].priceAtPurchase` = `75000` (snapshot giá)

> 💾 Lưu: `ORDER_ID_1 = data.id`

---

### TC-ORDER-02 ✅ Tạo order TAKEAWAY

```
POST /api/orders
Authorization: Bearer {TOKEN_SERVER}
X-User-Id: 2

{
  "tableId": null,
  "type": "TAKEAWAY",
  "items": [{ "menuItemId": 5, "quantity": 1, "specialNotes": null, "options": {} }]
}
```

> 💾 Lưu: `ORDER_ID_TAKEAWAY = data.id`

---

### TC-ORDER-03 ❌ Tạo order với item INACTIVE

```
// Đổi MENU_ITEM_ID_NEW sang INACTIVE trước
PATCH /api/menu/{MENU_ITEM_ID_NEW}/status → { "status": "INACTIVE" }

// Thử order
POST /api/orders → { "items": [{ "menuItemId": {MENU_ITEM_ID_NEW} }] }
```

**Kết quả mong đợi:** HTTP `400 Bad Request`

---

### TC-ORDER-04 ✅ Hủy order TAKEAWAY

```
DELETE /api/orders/{ORDER_ID_TAKEAWAY}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** `data.status: "CANCELLED"`

---

## 7. Module KITCHEN — Nhà bếp & Station

> Sau TC-ORDER-01, `OrderPlacedEvent` tự động tạo `KitchenTicket` cho từng `OrderItem`.

### TC-KITCHEN-01 ✅ Xem toàn bộ queue

```
GET /api/kitchen/queue
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- Tickets từ `ORDER_ID_1` với `status: "QUEUED"`
- `assignedStation: null` (chưa chọn station)

> 💾 Lưu: `TICKET_ID_1`, `TICKET_ID_2` = id của 2 tickets

---

### TC-KITCHEN-02 ✅ Xem tất cả station AVAILABLE

```
GET /api/kitchen/stations
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- Array 9 stations từ seed, tất cả `status: "AVAILABLE"`
- Mỗi station có: `id`, `name`, `type`, `status`

> 💾 Lưu: `STATION_ID_GRILL` = id của một station type `GRILL`

---

### TC-KITCHEN-03 ✅ Lọc station theo type

```
GET /api/kitchen/stations?type=GRILL
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- Tất cả stations trả về đều `type: "GRILL"` và `status: "AVAILABLE"`

> 💾 Lưu: `STATION_ID_GRILL_2` = id của station GRILL thứ hai

---

### TC-KITCHEN-04 ✅ Bắt đầu nấu — bắt buộc chọn station (QUEUED → COOKING)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/start?stationId={STATION_ID_GRILL}
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "COOKING"`
- `data.startedAt` được set
- `data.assignedStation.id` = `STATION_ID_GRILL`
- `data.assignedStation.status: "IN_USE"`

---

### TC-KITCHEN-05 ✅ Station đã bị đánh IN_USE sau khi assign

```
GET /api/kitchen/stations?type=GRILL
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `STATION_ID_GRILL` **không còn xuất hiện** (chỉ trả AVAILABLE)

---

### TC-KITCHEN-06 ❌ Không thể start ticket thiếu stationId

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/start
Authorization: Bearer {TOKEN_CHEF}
(không có ?stationId=)
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request` — `stationId` là required param

---

### TC-KITCHEN-07 ❌ Không thể dùng station đang IN_USE

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/start?stationId={STATION_ID_GRILL}
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"already IN_USE"`

---

### TC-KITCHEN-08 ✅ Bắt đầu ticket 2 với station khác

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/start?stationId={STATION_ID_GRILL_2}
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "COOKING"`
- `data.assignedStation.id` = `STATION_ID_GRILL_2`

---

### TC-KITCHEN-09 ✅ Tạm dừng — station tự động giải phóng (COOKING → PAUSED)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/pause
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "PAUSED"`
- `data.assignedStation: null`

**Kiểm tra thêm:**
```
GET /api/kitchen/stations?type=GRILL
```
- `STATION_ID_GRILL` xuất hiện lại với `status: "AVAILABLE"`

---

### TC-KITCHEN-10 ✅ Resume từ PAUSED — chọn lại station (PAUSED → COOKING)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/start?stationId={STATION_ID_GRILL}
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "COOKING"`
- `data.assignedStation.id` = `STATION_ID_GRILL` (re-assigned)

---

### TC-KITCHEN-11 ✅ Hoàn tác COOKING → QUEUED — station giải phóng

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/undo
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "QUEUED"`
- `data.startedAt: null`
- `data.assignedStation: null`
- `STATION_ID_GRILL_2` trở về AVAILABLE

---

### TC-KITCHEN-12 ✅ Hoàn thành nấu (COOKING → READY) — station giải phóng

> `TICKET_ID_1` đang COOKING với `STATION_ID_GRILL`.

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/done
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "READY"`
- `data.finishedAt` được set
- `data.assignedStation: null`
- `STATION_ID_GRILL` trở về `AVAILABLE`
- **Side effect (async):** `KitchenItemDoneEvent` → inventory tự động trừ kho

---

### TC-KITCHEN-13 ✅ Hoàn tác READY → COOKING (không cần re-assign station ngay)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/undo
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- `data.status: "COOKING"`
- `data.finishedAt: null`
- `data.assignedStation: null` (chef cần chọn lại khi tiếp tục)

---

### TC-KITCHEN-14 ❌ Pause từ QUEUED — transition không hợp lệ

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/pause
Authorization: Bearer {TOKEN_CHEF}
// TICKET_ID_2 đang QUEUED sau undo ở TC-KITCHEN-11
```

**Kết quả mong đợi:**
- HTTP `409` hoặc `500`
- `message` chứa `"Cannot pause from state: QUEUED"`

---

### TC-KITCHEN-15 ✅ Hoàn thành cả 2 tickets để tiếp tục test payment

```
// TICKET_ID_1: chọn station → start → done
GET /api/kitchen/stations?type=GRILL   → lấy STATION_ID_GRILL
PATCH .../TICKET_ID_1/start?stationId={STATION_ID_GRILL}
PATCH .../TICKET_ID_1/done

// TICKET_ID_2: chọn station → start → done
GET /api/kitchen/stations?type=GRILL   → lấy STATION_ID_GRILL_2
PATCH .../TICKET_ID_2/start?stationId={STATION_ID_GRILL_2}
PATCH .../TICKET_ID_2/done
```

**Kết quả mong đợi:** Cả 2 tickets → `status: "READY"`, cả 2 stations → `AVAILABLE`

---

## 8. Module INVENTORY — Kho nguyên liệu

### TC-INV-01 ✅ Xem cảnh báo tồn kho thấp

```
GET /api/inventory/alerts
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- Mỗi alert có: `ingredientId`, `name`, `currentStock`, `minThreshold`, `unit`

---

### TC-INV-02 ✅ Nhập thủ công hao hụt

```
POST /api/inventory/usage
Authorization: Bearer {TOKEN_CHEF}
X-User-Id: 3

[
  { "ingredientId": 1, "amount": 0.500, "reason": "SPOILAGE" },
  { "ingredientId": 5, "amount": 0.200, "reason": "ADJUSTMENT" }
]
```

**Kết quả mong đợi:** HTTP `200 OK`, `message: "Usage recorded"`

---

### TC-INV-03 ❌ Nhập lượng zero

```
POST /api/inventory/usage
[{ "ingredientId": 1, "amount": 0, "reason": "SPOILAGE" }]
```

**Kết quả mong đợi:** HTTP `400 Bad Request`

---

### TC-INV-04 ✅ Xác nhận auto-deduct sau TC-KITCHEN-12

> Chờ 1-2 giây (async) rồi kiểm tra:

```
GET /api/inventory/alerts
```

**Kết quả mong đợi:** Tồn kho nguyên liệu làm Phở Bò đã giảm theo recipe

---

## 9. Module PAYMENT — Thanh toán

### TC-PAY-01 ✅ Xem tổng bill

```
GET /api/payments/orders/{ORDER_ID_1}/total
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- `data.subTotal: 220000`
- `data.taxAmount` = `(subTotal - discount) × 10%`
- `data.total` = `subTotal - discount + tax`

---

### TC-PAY-02 ✅ Thanh toán tiền mặt

```
POST /api/payments?orderId={ORDER_ID_1}&amount=242000&method=CASH&tip=20000
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- `data.status: "COMPLETED"`
- **Side effect:** Order → `PAID`, Table → `DIRTY`

> 💾 Lưu: `ORDER_ID_PAID = ORDER_ID_1`

---

### TC-PAY-03 ✅ Kiểm tra order đã PAID

```
GET /api/orders/{ORDER_ID_PAID}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** `data.status: "PAID"`

---

### TC-PAY-04 ✅ Kiểm tra bàn chuyển DIRTY

```
GET /api/seating/tables
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** Bàn `TABLE_ID_SEATED` có `status: "DIRTY"`

---

### TC-PAY-05 ❌ Thanh toán lần 2 cho order đã PAID

```
POST /api/payments?orderId={ORDER_ID_PAID}&amount=220000&method=CASH
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"already paid"`

---

### TC-PAY-06 ✅ Hoàn tiền (refund)

```
// Tạo order mới + thanh toán → PAYMENT_TX_ID = data.id
POST /api/payments?orderId={ORDER_ID_NEW}&amount=75000&method=CARD
// → 💾 PAYMENT_TX_ID

POST /api/payments/{PAYMENT_TX_ID}/refund
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:** `data.status: "REFUNDED"`

---

## 10. Module REPORT — Báo cáo

### TC-REPORT-01 ✅ Báo cáo doanh thu (SALE)

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{ "type": "SALE", "startDate": "2026-04-01T00:00:00", "dueDate": "2026-04-30T23:59:59" }
```

**Kết quả mong đợi:**
- `data.dataPoints.totalOrders` ≥ 1
- `data.dataPoints.totalRevenue` > 0

---

### TC-REPORT-02 ✅ Báo cáo SLA bếp

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{ "type": "SLA", "startDate": "2026-04-01T00:00:00", "dueDate": "2026-04-30T23:59:59" }
```

**Kết quả mong đợi:**
- `data.dataPoints.totalTickets` ≥ 2
- `data.dataPoints.slaBreachPercent` từ 0-100

---

### TC-REPORT-03 ✅ Xuất báo cáo (export)

```
POST /api/reports/export
Authorization: Bearer {TOKEN_MANAGER}

{ "type": "SALE", "startDate": "2026-04-01T00:00:00", "dueDate": "2026-04-30T23:59:59" }
```

**Kết quả mong đợi:** `data` là string URL dạng `/exports/sale-{timestamp}.json`

---

### TC-REPORT-04 ❌ SERVER không thể xem báo cáo

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:** HTTP `403 Forbidden`

---

## 11. Kiểm thử Bảo mật & Phân quyền

### TC-SEC-01 ❌ JWT giả mạo

```
GET /api/menu
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.FAKE.SIGNATURE
```

**Kết quả mong đợi:**
- HTTP `401 Unauthorized`
- Response là **JSON** đúng format (không phải trang HTML Tomcat)

---

### TC-SEC-02 ❌ Không có token

```
GET /api/orders/1
(không có Authorization)
```

**Kết quả mong đợi:**
- HTTP `401`, `message` chứa `"Unauthenticated"`

---

### TC-SEC-03 Matrix phân quyền đầy đủ

| Endpoint | MANAGER | SERVER | CHEF | CASHIER |
|----------|---------|--------|------|---------|
| `POST /api/menu` | ✅ 201 | ❌ 403 | ❌ 403 | ❌ 403 |
| `POST /api/orders` | ✅ 201 | ✅ 201 | ❌ 403 | ❌ 403 |
| `PATCH /api/kitchen/tickets/{id}/start` | ✅ 200 | ❌ 403 | ✅ 200 | ❌ 403 |
| `GET /api/kitchen/stations` | ✅ 200 | ❌ 403 | ✅ 200 | ❌ 403 |
| `POST /api/payments` | ✅ 201 | ❌ 403 | ❌ 403 | ✅ 201 |
| `POST /api/reports/dashboard` | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 |
| `GET /api/inventory/alerts` | ✅ 200 | ✅ 200 | ✅ 200 | ❌ 403 |

---

## 12. Kiểm thử Luồng End-to-End

### E2E-01 — Luồng Dine-in hoàn chỉnh (bao gồm Station)

```
1.  [MANAGER] Tạo menu item mới
2.  [MANAGER] Tạo promotion BY_PERCENT
3.  [SERVER]  Walk-in → seat bàn
4.  [SERVER]  Tạo order → PLACED
                └─ OrderPlacedEvent → KitchenTicket (QUEUED)
5.  [CHEF]   GET /api/kitchen/stations?type=GRILL → chọn STATION_ID
6.  [CHEF]   GET /api/kitchen/queue → thấy tickets QUEUED
7.  [CHEF]   PATCH .../start?stationId={STATION_ID} → COOKING, station IN_USE
8.  [CHEF]   PATCH .../done → READY, station AVAILABLE
                └─ KitchenItemDoneEvent (async) → inventory auto-deduct
9.  [CASHIER] GET /api/payments/orders/{id}/total → xem discount + tax
10. [CASHIER] POST /api/payments (CASH) → PAID, Table DIRTY
11. [MANAGER] POST /api/reports/dashboard (SALE) → thấy revenue
```

**Assertions cuối:** Order = `PAID` · Table = `DIRTY` · Station = `AVAILABLE`

---

### E2E-02 — Luồng station bị tranh chấp

```
1.  Tạo 2 orders → 4 tickets QUEUED
2.  Chef-A: start ticket-1 với STATION_GRILL_1 → IN_USE
3.  Chef-B: start ticket-2 với STATION_GRILL_1 → ❌ 409 "already IN_USE"
4.  Chef-B: start ticket-2 với STATION_GRILL_2 → ✅ OK
5.  Chef-A: done ticket-1 → STATION_GRILL_1 AVAILABLE
6.  Chef-B: start ticket-3 với STATION_GRILL_1 (vừa freed) → ✅ OK
```

---

### E2E-03 — Luồng Pause/Resume với station rotation

```
1.  Ticket-1 start với S1 → COOKING (S1 IN_USE)
2.  Pause ticket-1 → PAUSED (S1 AVAILABLE)
3.  Ticket-2 start với S1 → COOKING (S1 IN_USE)
4.  Resume ticket-1: GET stations → S1 bận → chọn S2
    PATCH ticket-1/start?stationId=S2 → COOKING (S2 IN_USE)
5.  Done ticket-2 → S1 AVAILABLE
6.  Done ticket-1 → S2 AVAILABLE
```

---

### E2E-04 — Luồng Split Bill

```
1.  Order 4 items → total 400,000
2.  Chef nấu xong tất cả (mỗi ticket chọn đúng station)
3.  POST /api/payments?amount=200000&method=CASH
4.  POST /api/payments?amount=200000&method=E_WALLET
    → 2 transactions COMPLETED, Order = PAID
```

---

### E2E-05 — Admin disable item đang bán

```
1.  [SERVER]  Order với item A → ORDER_ACTIVE (PLACED)
2.  [MANAGER] DELETE /api/menu/{itemA_id}
              → ❌ 409: "part of an active order"
3.  [CHEF]   Nấu xong + [CASHIER] Thanh toán → PAID
4.  [MANAGER] DELETE /api/menu/{itemA_id} → ✅ OK
5.  [SERVER]  Order với item A → ❌ 400: "not ACTIVE"
```

---

## 13. Kiểm thử Nghiệp vụ Đặc biệt

### TC-BIZ-01 ✅ Allergy Alert tự động

```
POST /api/orders
{ "items": [{ "menuItemId": 1, "quantity": 1,
              "specialNotes": "Dị ứng gluten", "options": {} }] }
```

**Kết quả mong đợi:** `data.items[0].isAllergyAlert: true`

---

### TC-BIZ-02 ❌ Tạo order khi inventory không đủ

```
// Nhập hao hụt toàn bộ thịt bò
POST /api/inventory/usage
[{ "ingredientId": 2, "amount": 10.000, "reason": "SPOILAGE" }]

// Order 100 tô Phở
POST /api/orders → { "items": [{ "menuItemId": 1, "quantity": 100 }] }
```

**Kết quả mong đợi:**
- HTTP `422 Unprocessable Entity`
- `message` chứa `"Insufficient stock"`

---

### TC-BIZ-03 ✅ Station OFFLINE không thể được chọn

```
// Station đã được set OFFLINE trong DB
PATCH /api/kitchen/tickets/{id}/start?stationId={STATION_OFFLINE_ID}
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"OFFLINE"`

---

### TC-BIZ-04 ✅ Discount không vượt subTotal

```
POST /api/promotions/simulate
{ "menuItemQuantities": { "8": 1 }, "subTotal": 35000 }
```

**Kết quả mong đợi:**
- `data.discountAmount` ≤ `35000`
- `data.finalTotal` ≥ 0

---

## Tổng kết Test Coverage

| Module | Happy Path | Error Cases | Role Guards | Business Logic |
|--------|-----------|-------------|-------------|----------------|
| AUTH | ✅ 5 | ✅ 2 | — | — |
| MENU | ✅ 6 | ✅ 2 | ✅ 1 | ✅ Soft delete |
| PROMOTION | ✅ 3 | ✅ 1 | — | ✅ Discount calc |
| SEATING | ✅ 4 | ✅ 1 | — | ✅ Auto-assign |
| ORDER | ✅ 3 | ✅ 2 | — | ✅ Facade validate |
| KITCHEN | ✅ 10 | ✅ 3 | ✅ 1 | ✅ State Machine + Station lifecycle |
| INVENTORY | ✅ 3 | ✅ 1 | — | ✅ Auto-deduct |
| PAYMENT | ✅ 5 | ✅ 1 | ✅ 1 | ✅ Refund |
| REPORT | ✅ 3 | ✅ 1 | ✅ 1 | ✅ 4 report types |
| SECURITY | — | ✅ 2 | ✅ 7 combos | — |
| **E2E** | ✅ 5 flows | — | — | ✅ Cross-domain |
| **BIZ** | ✅ 2 | ✅ 2 | — | ✅ Edge cases |

**Tổng:** ~83 test cases — bao phủ toàn bộ luồng từ auth → kitchen station → report.

---

> 📝 **Tài khoản mock:** Tất cả dùng password `123456`
> 🔗 **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`