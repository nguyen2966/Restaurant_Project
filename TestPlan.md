# TEST PLAN — Hệ Thống Quản Lý Nhà Hàng

> **Phiên bản:** 1.0 | **Ngày:** 2025-04 | **Môi trường:** `http://localhost:8080`
> **Công cụ:** Swagger UI (`/swagger-ui/index.html`) hoặc Postman

---

## Mục Lục

1. [Quy ước & Chuẩn bị](#1-quy-ước--chuẩn-bị)
2. [Module AUTH — Xác thực](#2-module-auth--xác-thực)
3. [Module MENU — Thực đơn](#3-module-menu--thực-đơn)
4. [Module PROMOTION — Khuyến mãi](#4-module-promotion--khuyến-mãi)
5. [Module SEATING — Bàn ghế & Đặt bàn](#5-module-seating--bàn-ghế--đặt-bàn)
6. [Module ORDER — Tạo đơn hàng](#6-module-order--tạo-đơn-hàng)
7. [Module KITCHEN — Nhà bếp](#7-module-kitchen--nhà-bếp)
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

### 1.2 Format Response chuẩn

Mọi response đều có dạng:
```json
{
  "success": true | false,
  "data": { ... } | null,
  "message": "..." | null,
  "timestamp": "2025-04-01T10:30:00"
}
```

### 1.3 Tài khoản test (từ seed data)

| Username | Password | Role |
|----------|----------|------|
| `admin` | `password123` | MANAGER |
| `server_1` | `password123` | SERVER |
| `chef_1` | `password123` | CHEF |
| `cashier_1` | `password123` | CASHIER |

### 1.4 Thứ tự thực hiện

> **Quan trọng:** Các test case có đánh số thứ tự phải chạy **tuần tự** vì dữ liệu phụ thuộc nhau.
> Chạy từ Section 2 → 12 theo thứ tự để cover toàn bộ luồng.

---

## 2. Module AUTH — Xác thực

### TC-AUTH-01 ✅ Đăng nhập thành công với MANAGER

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `success: true`
- `data.token` là chuỗi JWT (bắt đầu bằng `eyJ`)
- `data.role` = `"MANAGER"`
- `data.expiresInSeconds` = `86400`

> 💾 **Lưu lại:** `TOKEN_MANAGER = data.token`

---

### TC-AUTH-02 ✅ Đăng nhập với các role khác

Lần lượt đăng nhập và lưu token:

```json
// SERVER
{ "username": "server_1", "password": "password123" }
// → Lưu TOKEN_SERVER

// CHEF
{ "username": "chef_1", "password": "password123" }
// → Lưu TOKEN_CHEF

// CASHIER
{ "username": "cashier_1", "password": "password123" }
// → Lưu TOKEN_CASHIER
```

**Kết quả mong đợi:** HTTP `200`, mỗi token có `role` đúng tương ứng.

---

### TC-AUTH-03 ❌ Đăng nhập sai mật khẩu

```json
{ "username": "admin", "password": "saimatkhau" }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `success: false`
- `message` chứa thông báo sai thông tin đăng nhập

---

### TC-AUTH-04 ❌ Gọi API không có token

```
GET /api/menu
(không có header Authorization)
```

**Kết quả mong đợi:**
- HTTP `401 Unauthorized`
- `success: false`
- `message` chứa `"Unauthenticated"`

---

### TC-AUTH-05 ✅ Validate token còn hiệu lực

```
POST /api/auth/validate
Authorization: Bearer {TOKEN_MANAGER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data: true`
- `message: "Token còn hiệu lực"`

---

## 3. Module MENU — Thực đơn

> Dùng `TOKEN_MANAGER` cho write operations, `TOKEN_SERVER` cho read.

### TC-MENU-01 ✅ Xem toàn bộ menu

```
GET /api/menu
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data` là array các `MenuItemResponse`
- Mỗi item có: `id`, `name`, `basePrice`, `status`
- Các item từ seed data hiển thị (Phở Bò, Cơm Tấm, ...)

---

### TC-MENU-02 ✅ Lọc theo status ACTIVE

```
GET /api/menu?status=ACTIVE
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Tất cả items trong response đều có `status: "ACTIVE"`

---

### TC-MENU-03 ✅ Tìm kiếm theo tên

```
GET /api/menu?query=phở
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Kết quả chứa các món có tên liên quan đến "phở"
- Tìm kiếm **không phân biệt hoa thường** (gõ "PHỞ" cũng ra kết quả)

---

### TC-MENU-04 ✅ Tạo món mới

```
POST /api/menu
Authorization: Bearer {TOKEN_MANAGER}
Content-Type: application/json

{
  "name": "Bún Chả Hà Nội",
  "basePrice": 75000,
  "description": "Bún chả đặc trưng Hà Nội với nước chấm pha chuẩn vị",
  "allergens": ["gluten"]
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.id` được tạo mới (lưu lại: `MENU_ITEM_ID_NEW = data.id`)
- `data.status: "ACTIVE"` (mặc định khi tạo mới)
- `data.name: "Bún Chả Hà Nội"`

---

### TC-MENU-05 ✅ Xem chi tiết một món

```
GET /api/menu/{MENU_ITEM_ID_NEW}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.id` = `MENU_ITEM_ID_NEW`
- `data.allergens` = `["gluten"]`

---

### TC-MENU-06 ✅ Cập nhật thông tin món

```
PUT /api/menu/{MENU_ITEM_ID_NEW}
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Bún Chả Hà Nội Đặc Biệt",
  "basePrice": 85000,
  "description": "Bún chả với thêm chả cốm",
  "allergens": ["gluten", "egg"]
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.name: "Bún Chả Hà Nội Đặc Biệt"`
- `data.basePrice: 85000`

---

### TC-MENU-07 ✅ Đổi trạng thái sang OUT_OF_STOCK

```
PATCH /api/menu/{MENU_ITEM_ID_NEW}/status
Authorization: Bearer {TOKEN_MANAGER}

{ "status": "OUT_OF_STOCK" }
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "OUT_OF_STOCK"`

---

### TC-MENU-08 ✅ Đổi trạng thái về ACTIVE

```
PATCH /api/menu/{MENU_ITEM_ID_NEW}/status
Authorization: Bearer {TOKEN_MANAGER}

{ "status": "ACTIVE" }
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "ACTIVE"`

---

### TC-MENU-09 ✅ Xem best sellers

```
GET /api/menu/best-sellers
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data` là array (có thể rỗng nếu chưa có order PAID)

---

### TC-MENU-10 ❌ Tạo món không có basePrice

```
POST /api/menu
Authorization: Bearer {TOKEN_MANAGER}

{ "name": "Thiếu giá" }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa `"Base price is required"`

---

### TC-MENU-11 ❌ SERVER không thể tạo món

```
POST /api/menu
Authorization: Bearer {TOKEN_SERVER}

{ "name": "Test", "basePrice": 50000 }
```

**Kết quả mong đợi:**
- HTTP `403 Forbidden`
- `message` chứa `"Access denied"`

---

## 4. Module PROMOTION — Khuyến mãi

> Lưu lại `MENU_ITEM_ID_PHO` = id của món "Phở Bò" từ seed data (thường là id=1).

### TC-PROMO-01 ✅ Tạo khuyến mãi BY_PERCENT

```
POST /api/promotions
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Giảm 20% Drinks Happy Hour",
  "promoType": "BY_PERCENT",
  "condition": "Áp dụng 14:00-17:00 mỗi ngày",
  "menuItemIds": [7, 8, 9],
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59",
  "discountValue": 20
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.id` (lưu: `PROMO_ID_PERCENT`)
- `data.status: "ACTIVE"`
- `data.promoType: "BY_PERCENT"`

---

### TC-PROMO-02 ✅ Tạo khuyến mãi COMBO

```
POST /api/promotions
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Combo Cơm + Trà Đá",
  "promoType": "COMBO",
  "condition": "Mua cả 2 món giảm 15,000đ",
  "menuItemIds": [5, 9],
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59",
  "discountValue": 15000
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.promoType: "COMBO"`

---

### TC-PROMO-03 ✅ Simulate discount BY_PERCENT

```
POST /api/promotions/simulate
Authorization: Bearer {TOKEN_MANAGER}

{
  "menuItemQuantities": { "7": 2, "8": 1 },
  "subTotal": 125000
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.discountAmount` = `25000` (20% của 125,000)
- `data.finalTotal` = `100000`
- `data.appliedPromo` chứa tên promo vừa tạo

---

### TC-PROMO-04 ✅ Xem danh sách promotion ACTIVE

```
GET /api/promotions?status=ACTIVE
Authorization: Bearer {TOKEN_MANAGER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array chứa các promo vừa tạo ở trên

---

### TC-PROMO-05 ✅ Cập nhật promotion

```
PUT /api/promotions/{PROMO_ID_PERCENT}
Authorization: Bearer {TOKEN_MANAGER}

{
  "name": "Giảm 25% Drinks Happy Hour",
  "promoType": "BY_PERCENT",
  "condition": "Áp dụng 14:00-17:00",
  "menuItemIds": [7, 8, 9],
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59",
  "discountValue": 25
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.discountValue: 25`

---

### TC-PROMO-06 ❌ Disable menu item đang trong active promo

> Test này kiểm tra `AdminCatalogFacade` guard.

```
DELETE /api/menu/7
Authorization: Bearer {TOKEN_MANAGER}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"referenced by an active promotion"`

---

### TC-PROMO-07 ✅ Đổi status promo sang INACTIVE

```
PATCH /api/promotions/{PROMO_ID_PERCENT}/status
Authorization: Bearer {TOKEN_MANAGER}

{ "status": "INACTIVE" }
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "INACTIVE"`

---

## 5. Module SEATING — Bàn ghế & Đặt bàn

### TC-SEAT-01 ✅ Xem sơ đồ toàn bộ bàn

```
GET /api/seating/tables
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array các bàn từ seed (T01, T02, T03, ...)
- Có ít nhất 1 bàn `status: "AVAILABLE"`
- Bàn T04 có `status: "OCCUPIED"` (từ seed)

> 💾 Lưu: `TABLE_ID_AVAILABLE` = id của một bàn AVAILABLE

---

### TC-SEAT-02 ✅ Nhận khách walk-in vào bàn

```
POST /api/seating/tables/{TABLE_ID_AVAILABLE}/seat-walkin?partySize=2
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "SEATED"`

> 💾 Lưu: `TABLE_ID_SEATED` = TABLE_ID_AVAILABLE

---

### TC-SEAT-03 ❌ Walk-in vào bàn đã có khách

```
POST /api/seating/tables/{TABLE_ID_SEATED}/seat-walkin?partySize=2
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"not available"`

---

### TC-SEAT-04 ✅ Đặt bàn trước (reservation)

```
POST /api/seating/reservations
Authorization: Bearer {TOKEN_SERVER}

{
  "customerName": "Trần Thị Bình",
  "customerPhone": "0987654321",
  "partySize": 4,
  "reservedTime": "2025-04-05T19:00:00"
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.status: "BOOKED"`
- `data.tableId` được auto-assign (hệ thống chọn bàn phù hợp)

> 💾 Lưu: `RESERVATION_ID = data.id`

---

### TC-SEAT-05 ✅ Check-in khách đặt bàn

```
POST /api/seating/reservations/{RESERVATION_ID}/seat
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "SEATED"`

---

### TC-SEAT-06 ✅ Thêm khách vào waitlist

```
POST /api/seating/waitlist
Authorization: Bearer {TOKEN_SERVER}

{
  "customerName": "Lê Văn Cường",
  "partySize": 3
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.isNotified: false`
- `data.joinedAt` được set

---

### TC-SEAT-07 ✅ Xem waitlist

```
GET /api/seating/waitlist
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Chứa khách vừa thêm ở TC-SEAT-06
- Sắp xếp theo `joinedAt` tăng dần (FIFO)

---

### TC-SEAT-08 ✅ Đặt bàn rồi đánh no-show

```
// Tạo reservation mới
POST /api/seating/reservations
{ "customerName": "No Show Guest", "partySize": 2, "reservedTime": "2025-04-02T12:00:00" }

// Đánh no-show
POST /api/seating/reservations/{id}/no-show
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "NO_SHOW"`

---

## 6. Module ORDER — Tạo đơn hàng

> Test core flow qua `OrderingFacade` — validate menu + inventory trước khi tạo.

### TC-ORDER-01 ✅ Tạo order DINE_IN thành công

```
POST /api/orders
Authorization: Bearer {TOKEN_SERVER}
X-User-Id: 2

{
  "tableId": {TABLE_ID_SEATED},
  "type": "DINE_IN",
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "specialNotes": "Ít hành",
      "options": { "spice": "mild" }
    },
    {
      "menuItemId": 8,
      "quantity": 2,
      "specialNotes": null,
      "options": {}
    }
  ]
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.status: "PLACED"` (OrderingFacade confirm ngay)
- `data.orderCode` dạng `"ORD-..."`
- `data.subTotal` = `(75000 × 2) + (35000 × 2)` = `220000`
- `data.items` có 2 phần tử
- `data.items[0].priceAtPurchase` = `75000` (snapshot giá)

> 💾 Lưu: `ORDER_ID_1 = data.id`, `ORDER_CODE_1 = data.orderCode`

---

### TC-ORDER-02 ✅ Tạo order TAKEAWAY (không cần tableId)

```
POST /api/orders
Authorization: Bearer {TOKEN_SERVER}
X-User-Id: 2

{
  "tableId": null,
  "type": "TAKEAWAY",
  "items": [
    { "menuItemId": 5, "quantity": 1, "specialNotes": null, "options": {} }
  ]
}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.tableId: null`
- `data.type: "TAKEAWAY"`
- `data.status: "PLACED"`

> 💾 Lưu: `ORDER_ID_TAKEAWAY = data.id`

---

### TC-ORDER-03 ✅ Xem chi tiết order

```
GET /api/orders/{ORDER_ID_1}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Tất cả fields khớp với order vừa tạo

---

### TC-ORDER-04 ✅ Xem tất cả orders của một bàn

```
GET /api/orders?tableId={TABLE_ID_SEATED}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array chứa `ORDER_ID_1`

---

### TC-ORDER-05 ✅ Thêm ghi chú cho order item

```
PATCH /api/orders/{ORDER_ID_1}/items/{items[0].id}/note
Authorization: Bearer {TOKEN_SERVER}

{ "note": "Khách dị ứng hành, tuyệt đối không cho hành" }
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.items[0].specialNotes` = `"Khách dị ứng hành, tuyệt đối không cho hành"`

---

### TC-ORDER-06 ❌ Tạo order với item INACTIVE

> Trước tiên đổi MENU_ITEM_ID_NEW (Bún Chả) sang INACTIVE, sau đó thử order.

```
// Bước 1: Đổi status
PATCH /api/menu/{MENU_ITEM_ID_NEW}/status
{ "status": "INACTIVE" }

// Bước 2: Thử tạo order
POST /api/orders
{ "type": "DINE_IN", "items": [{ "menuItemId": {MENU_ITEM_ID_NEW}, "quantity": 1 }] }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa id của item không ACTIVE

---

### TC-ORDER-07 ❌ Tạo order không có items

```
POST /api/orders
Authorization: Bearer {TOKEN_SERVER}

{ "tableId": 1, "type": "DINE_IN", "items": [] }
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa `"At least one item is required"`

---

### TC-ORDER-08 ✅ Hủy order

```
DELETE /api/orders/{ORDER_ID_TAKEAWAY}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "CANCELLED"`

---

### TC-ORDER-09 ❌ Hủy order đã PAID (sau TC-PAYMENT-01)

> Thực hiện sau khi đã có order PAID.

```
DELETE /api/orders/{ORDER_ID_PAID}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"Paid orders cannot be cancelled"`

---

## 7. Module KITCHEN — Nhà bếp

> Sau TC-ORDER-01, hệ thống tự động tạo `KitchenTicket` qua `OrderPlacedEvent`.

### TC-KITCHEN-01 ✅ Xem toàn bộ queue (không filter)

```
GET /api/kitchen/queue
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Chứa tickets từ `ORDER_ID_1` với `status: "QUEUED"`
- Có trường `nearDeadline`, `deadlineTime`

> 💾 Lưu: `TICKET_ID_1` và `TICKET_ID_2` = id của 2 tickets

---

### TC-KITCHEN-02 ✅ Filter queue theo status QUEUED

```
GET /api/kitchen/queue?status=QUEUED&sortBy=deadline
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Tất cả tickets đều `status: "QUEUED"`
- Sắp xếp theo `deadlineTime` tăng dần

---

### TC-KITCHEN-03 ✅ Bắt đầu nấu (QUEUED → COOKING)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/start
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "COOKING"`
- `data.startedAt` được set (≈ thời điểm hiện tại)

---

### TC-KITCHEN-04 ✅ Tạm dừng (COOKING → PAUSED)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/pause
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "PAUSED"`

---

### TC-KITCHEN-05 ✅ Tiếp tục nấu từ PAUSED (PAUSED → COOKING)

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/start
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "COOKING"`

---

### TC-KITCHEN-06 ✅ Hoàn tác về QUEUED (COOKING → QUEUED)

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/start  // Bắt đầu ticket 2 trước
PATCH /api/kitchen/tickets/{TICKET_ID_2}/undo   // Rồi undo về QUEUED
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi (sau undo):**
- HTTP `200 OK`
- `data.status: "QUEUED"`
- `data.startedAt: null` (đã reset)

---

### TC-KITCHEN-07 ✅ Hoàn thành nấu (COOKING → READY)

> Thực hiện với `TICKET_ID_1` đang ở COOKING.

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/done
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "READY"`
- `data.finishedAt` được set
- **Side effect (async):** `KitchenItemDoneEvent` published → inventory tự động bị trừ

---

### TC-KITCHEN-08 ✅ Hoàn tác từ READY về COOKING

```
PATCH /api/kitchen/tickets/{TICKET_ID_1}/undo
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.status: "COOKING"`
- `data.finishedAt: null` (đã reset)

---

### TC-KITCHEN-09 ❌ Pause từ QUEUED (transition không hợp lệ)

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/pause
Authorization: Bearer {TOKEN_CHEF}
// TICKET_ID_2 đang ở QUEUED sau undo
```

**Kết quả mong đợi:**
- HTTP `500` hoặc `409`
- `message` chứa `"Cannot pause from state: QUEUED"`

---

### TC-KITCHEN-10 ✅ Hoàn thành ticket 2 (để test payment sau)

```
PATCH /api/kitchen/tickets/{TICKET_ID_2}/start
PATCH /api/kitchen/tickets/{TICKET_ID_2}/done
// Và TICKET_ID_1: start → done
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:** Cả 2 tickets → `status: "READY"`

---

## 8. Module INVENTORY — Kho nguyên liệu

### TC-INV-01 ✅ Xem cảnh báo tồn kho thấp

```
GET /api/inventory/alerts
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array (có thể rỗng hoặc chứa items nếu sau các auto-deduct)
- Mỗi alert có: `ingredientId`, `name`, `currentStock`, `minThreshold`, `unit`

---

### TC-INV-02 ✅ Nhập thủ công hao hụt nguyên liệu

```
POST /api/inventory/usage
Authorization: Bearer {TOKEN_CHEF}
X-User-Id: 3

[
  {
    "ingredientId": 1,
    "amount": 0.500,
    "reason": "SPOILAGE"
  },
  {
    "ingredientId": 5,
    "amount": 0.200,
    "reason": "ADJUSTMENT"
  }
]
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `message: "Usage recorded"`
- Kiểm tra lại bằng TC-INV-01: `currentStock` của ingredient 1 giảm đi 0.5

---

### TC-INV-03 ❌ Nhập âm hoặc số 0

```
POST /api/inventory/usage
[{ "ingredientId": 1, "amount": 0, "reason": "SPOILAGE" }]
```

**Kết quả mong đợi:**
- HTTP `400 Bad Request`
- `message` chứa `"amount must be positive"`

---

### TC-INV-04 ✅ Xác nhận auto-deduct sau TC-KITCHEN-07

> Sau khi ticket DONE, `KitchenItemDoneEvent` trigger auto-deduct async.
> Chờ 1-2 giây rồi kiểm tra:

```
GET /api/inventory/alerts
Authorization: Bearer {TOKEN_CHEF}
```

**Kết quả mong đợi:**
- Tồn kho của nguyên liệu làm Phở Bò (bánh phở, thịt bò, nước dùng) đã giảm tương ứng với `requiredAmount × quantity` trong recipe

---

## 9. Module PAYMENT — Thanh toán

### TC-PAY-01 ✅ Xem tổng bill với discount

> Trước tiên kích hoạt lại promo BY_PERCENT nếu đã tắt.

```
GET /api/payments/orders/{ORDER_ID_1}/total
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.subTotal: 220000`
- `data.discountAmount` ≥ 0 (nếu có promo active cho items trong order)
- `data.taxAmount` = `(subTotal - discount) × 10%`
- `data.total` = `subTotal - discount + tax`

---

### TC-PAY-02 ✅ Thanh toán tiền mặt

```
POST /api/payments?orderId={ORDER_ID_1}&amount=242000&method=CASH&tip=20000
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- HTTP `201 Created`
- `data.status: "COMPLETED"`
- `data.method: "CASH"`
- `data.tipAmount: 20000`
- `data.gatewayReferenceId` được set (dạng `"GW-..."`)
- **Side effect:** `PaymentCompletedEvent` → `OrderService.markAsPaid()` → Order status → `PAID`
- **Side effect:** `OrderPaidEvent` → `SeatingService.clearTable()` → Table → `DIRTY`

---

### TC-PAY-03 ✅ Kiểm tra order đã PAID sau thanh toán

```
GET /api/orders/{ORDER_ID_1}
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- `data.status: "PAID"`

> 💾 Lưu: `ORDER_ID_PAID = ORDER_ID_1`

---

### TC-PAY-04 ✅ Kiểm tra bàn đã chuyển DIRTY sau thanh toán

```
GET /api/seating/tables
Authorization: Bearer {TOKEN_SERVER}
```

**Kết quả mong đợi:**
- Bàn `TABLE_ID_SEATED` có `status: "DIRTY"`

---

### TC-PAY-05 ❌ Thanh toán lần 2 cho order đã PAID

```
POST /api/payments?orderId={ORDER_ID_PAID}&amount=220000&method=CASH
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- HTTP `409 Conflict`
- `message` chứa `"already paid"`

---

### TC-PAY-06 ✅ Xem lịch sử payment của order

```
GET /api/payments/orders/{ORDER_ID_PAID}
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- Array chứa 1 transaction `status: "COMPLETED"`

---

### TC-PAY-07 ✅ Hoàn tiền (refund)

> Tạo một order mới, thanh toán, rồi refund để test.

```
// Tạo order mới
POST /api/orders ... → ORDER_ID_REFUND

// Thanh toán
POST /api/payments?orderId={ORDER_ID_REFUND}&amount=75000&method=CARD
→ PAYMENT_TX_ID = data.id

// Refund
POST /api/payments/{PAYMENT_TX_ID}/refund
Authorization: Bearer {TOKEN_CASHIER}
```

**Kết quả mong đợi (refund):**
- HTTP `200 OK`
- `data.status: "REFUNDED"`

---

### TC-PAY-08 ❌ CASHIER không thể tạo order (sai role)

```
POST /api/orders
Authorization: Bearer {TOKEN_CASHIER}
{ "type": "DINE_IN", "items": [...] }
```

**Kết quả mong đợi:**
- HTTP `403 Forbidden`

---

## 10. Module REPORT — Báo cáo

### TC-REPORT-01 ✅ Báo cáo doanh thu (SALE)

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{
  "type": "SALE",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.summary` chứa thông tin doanh thu
- `data.dataPoints.totalOrders` ≥ 1 (từ các order PAID trong test)
- `data.dataPoints.totalRevenue` > 0
- `data.dataPoints.averageOrderValue` > 0

---

### TC-REPORT-02 ✅ Báo cáo menu (MENU)

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{
  "type": "MENU",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.dataPoints.topSellers` là array các món bán chạy
- `data.dataPoints.totalMenuItemsSold` ≥ 2

---

### TC-REPORT-03 ✅ Báo cáo SLA bếp

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{
  "type": "SLA",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.dataPoints.totalTickets` ≥ 2
- `data.dataPoints.slaBreachPercent` là số từ 0-100

---

### TC-REPORT-04 ✅ Báo cáo hiệu suất bàn (TABLE)

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_MANAGER}

{
  "type": "TABLE",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data.dataPoints.tables` là array các bàn

---

### TC-REPORT-05 ✅ Xuất báo cáo (export)

```
POST /api/reports/export
Authorization: Bearer {TOKEN_MANAGER}

{
  "type": "SALE",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Kết quả mong đợi:**
- HTTP `200 OK`
- `data` là string URL dạng `/exports/sale-{timestamp}.json`
- `message` chứa tóm tắt báo cáo

---

### TC-REPORT-06 ❌ SERVER không thể xem báo cáo

```
POST /api/reports/dashboard
Authorization: Bearer {TOKEN_SERVER}
{ "type": "SALE", ... }
```

**Kết quả mong đợi:**
- HTTP `403 Forbidden`

---

## 11. Kiểm thử Bảo mật & Phân quyền

### TC-SEC-01 ❌ Gọi API với JWT giả mạo

```
GET /api/menu
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.FAKE.SIGNATURE
```

**Kết quả mong đợi:**
- HTTP `401 Unauthorized`

---

### TC-SEC-02 ❌ Sửa payload JWT (không thay đổi signature)

> Decode JWT, đổi role thành MANAGER, re-encode mà không ký lại.

**Kết quả mong đợi:**
- HTTP `401 Unauthorized`
- JWT signature validation fail

---

### TC-SEC-03 Kiểm tra phân quyền đầy đủ

| Endpoint | MANAGER | SERVER | CHEF | CASHIER |
|----------|---------|--------|------|---------|
| `POST /api/menu` | ✅ 201 | ❌ 403 | ❌ 403 | ❌ 403 |
| `POST /api/orders` | ✅ 201 | ✅ 201 | ❌ 403 | ❌ 403 |
| `PATCH /api/kitchen/tickets/{id}/done` | ✅ 200 | ❌ 403 | ✅ 200 | ❌ 403 |
| `POST /api/payments` | ✅ 201 | ❌ 403 | ❌ 403 | ✅ 201 |
| `POST /api/reports/dashboard` | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 |
| `GET /api/inventory/alerts` | ✅ 200 | ✅ 200 | ✅ 200 | ❌ 403 |

---

## 12. Kiểm thử Luồng End-to-End

### E2E-01 — Luồng Dine-in hoàn chỉnh

Đây là luồng quan trọng nhất, bao gồm toàn bộ hệ thống:

```
1.  [MANAGER] Tạo menu item mới
2.  [MANAGER] Tạo promotion BY_PERCENT cho item đó
3.  [SERVER]  Xem sơ đồ bàn → chọn bàn AVAILABLE
4.  [SERVER]  Seat walk-in customer (bàn → SEATED)
5.  [SERVER]  Tạo order với item vừa tạo → status PLACED
                └─ OrderingFacade: validate menu ACTIVE + check stock
                └─ OrderPlacedEvent → KitchenTicket tạo tự động (QUEUED)
6.  [SERVER]  Thêm ghi chú cho order item
7.  [CHEF]   Xem kitchen queue → thấy tickets
8.  [CHEF]   Bắt đầu nấu (QUEUED → COOKING)
9.  [CHEF]   Hoàn thành (COOKING → READY)
                └─ KitchenItemDoneEvent (async) → inventory auto-deduct
10. [CASHIER] Xem total bill → discount từ promotion áp dụng
11. [CASHIER] Thanh toán CASH
                └─ PaymentCompletedEvent → Order PAID
                └─ OrderPaidEvent → Table DIRTY
12. [MANAGER] Xem báo cáo SALE → thấy revenue từ order này
13. [MANAGER] Xem báo cáo SLA → thấy thời gian hoàn thành ticket
```

**Assertions cuối luồng:**
- Order status = `PAID`
- Bàn status = `DIRTY`
- Inventory bị trừ đúng theo recipe
- Report SALE có order này

---

### E2E-02 — Luồng Đặt bàn trước

```
1. [SERVER] Tạo reservation (partySize=6, reservedTime tương lai)
              └─ tableId auto-assigned (bàn T05 có capacity=6)
2. [SERVER] Đúng giờ: seat reservation → Table SEATED
3. [SERVER] Tạo order cho bàn đó
4. ... (tiếp tục như E2E-01 từ bước 7)
```

---

### E2E-03 — Luồng Split Bill

```
1.  [SERVER]  Tạo order với 4 items
2.  [CHEF]   Nấu xong tất cả tickets
3.  [CASHIER] Xem total = 400,000đ
4.  [CASHIER] Split bill: 2 người × 200,000đ
              POST /api/payments?orderId=...&amount=200000&method=CASH
              POST /api/payments?orderId=...&amount=200000&method=E_WALLET
5.  [CASHIER] Xem lịch sử: GET /api/payments/orders/{id}
              → 2 transactions COMPLETED
6.  Kiểm tra: order status = PAID
```

---

### E2E-04 — Luồng Admin vô hiệu hóa menu item đang bán

```
1. [SERVER]  Tạo order chứa item A (item A đang ACTIVE)
             → ORDER_ACTIVE chứa item A, status PLACED
2. [MANAGER] Cố disable item A
             DELETE /api/menu/{itemA_id}
             → ❌ 409 Conflict: "part of an active order"
3. [CASHIER] Thanh toán ORDER_ACTIVE → PAID
4. [MANAGER] Disable item A lại
             → ✅ 200 OK (không còn active order)
5. [SERVER]  Thử tạo order mới với item A
             → ❌ 400 Bad Request: "not ACTIVE"
```

---

### E2E-05 — Luồng Promo Combo

```
1. [MANAGER] Tạo COMBO promo: menuItemIds=[5,9], discount=15000
             (Cơm Tấm + Trà Đá)
2. [MANAGER] Simulate: { "7": 1, "9": 1 } (chỉ có item 9, không phải combo đủ)
             → discountAmount = 0 (không đủ điều kiện combo)
3. [MANAGER] Simulate: { "5": 1, "9": 1 } (đủ combo)
             → discountAmount = 15000 ✅
4. [SERVER]  Tạo order với items 5 và 9
5. [CASHIER] Xem total → discount 15000 được áp dụng
```

---

## 13. Kiểm thử Nghiệp vụ Đặc biệt

### TC-BIZ-01 ✅ Allergy Alert tự động

```
POST /api/orders
{
  "items": [{
    "menuItemId": 1,          // Phở Bò — allergens: ["gluten"]
    "quantity": 1,
    "specialNotes": "Khách có tiền sử dị ứng gluten",
    "options": {}
  }]
}
```

**Kết quả mong đợi:**
- `data.items[0].isAllergyAlert: true` (vì specialNotes + allergens cùng tồn tại)

---

### TC-BIZ-02 ✅ Không có recipe — inventory không bị trừ

> Tạo một menu item **không có recipe** (không có row trong `recipe_item`), order và nấu xong.

**Kết quả mong đợi:**
- Ticket → READY thành công
- Warning log: `"No recipe found for menuItemId=... Skipping inventory deduction"`
- Inventory không thay đổi

---

### TC-BIZ-03 ❌ Tạo order khi inventory không đủ

> Giả lập hết nguyên liệu: nhập manual usage = toàn bộ tồn kho của thịt bò.

```
// Nhập hao hụt toàn bộ thịt bò (ingredient id=2, giả sử còn 10kg)
POST /api/inventory/usage
[{ "ingredientId": 2, "amount": 10.000, "reason": "SPOILAGE" }]

// Thử order 100 tô Phở (cần 15kg thịt bò)
POST /api/orders
{ "items": [{ "menuItemId": 1, "quantity": 100 }] }
```

**Kết quả mong đợi:**
- HTTP `422 Unprocessable Entity`
- `message` chứa `"Insufficient stock"` và tên nguyên liệu thiếu

---

### TC-BIZ-04 ✅ Kitchen State Machine — đầy đủ happy path

```
QUEUED → start → COOKING
COOKING → done → READY ✅
```

```
QUEUED → start → COOKING
COOKING → pause → PAUSED
PAUSED → start → COOKING
COOKING → undo → QUEUED
QUEUED → start → COOKING
COOKING → done → READY ✅
```

---

### TC-BIZ-05 ✅ Discount không vượt subTotal

```
POST /api/promotions/simulate
{
  "menuItemQuantities": { "8": 1 },
  "subTotal": 35000
}
```

> Promo giảm 100% nếu có — discountAmount không được vượt 35000.

**Kết quả mong đợi:**
- `data.discountAmount` ≤ `data.originalSubTotal`
- `data.finalTotal` ≥ 0

---

### TC-BIZ-06 ✅ Xem menu với case-insensitive enum

```
GET /api/menu?status=active      // lowercase
GET /api/menu?status=ACTIVE      // uppercase
GET /api/menu?status=Active      // mixed
```

**Kết quả mong đợi:** Cả 3 request đều trả về `200 OK` cùng kết quả.

---

### TC-BIZ-07 ✅ Null fields không xuất hiện trong response

```
GET /api/orders/{ORDER_ID_PAID}
```

**Kết quả mong đợi:**
- Fields `null` không xuất hiện trong JSON response (nhờ `JsonInclude.NON_NULL`)
- Ví dụ: nếu `specialNotes` là null thì key đó không có trong response

---

## Tổng kết Test Coverage

| Module | Happy Path | Error Cases | Role Guards | Business Logic |
|--------|-----------|-------------|-------------|----------------|
| AUTH | ✅ 5 | ✅ 2 | — | — |
| MENU | ✅ 7 | ✅ 2 | ✅ 1 | ✅ Soft delete |
| PROMOTION | ✅ 5 | ✅ 1 | — | ✅ Discount calc |
| SEATING | ✅ 6 | ✅ 1 | — | ✅ Auto-assign |
| ORDER | ✅ 5 | ✅ 3 | ✅ 1 | ✅ Facade validate |
| KITCHEN | ✅ 7 | ✅ 1 | — | ✅ State Machine |
| INVENTORY | ✅ 3 | ✅ 1 | — | ✅ Auto-deduct |
| PAYMENT | ✅ 5 | ✅ 2 | ✅ 1 | ✅ Split bill |
| REPORT | ✅ 5 | ✅ 1 | ✅ 1 | ✅ All 4 types |
| SECURITY | — | ✅ 3 | ✅ 6 combos | — |
| **E2E** | ✅ 5 flows | — | — | ✅ Cross-domain |
| **BIZ** | ✅ 4 | ✅ 2 | — | ✅ Edge cases |

**Tổng:** ~80 test cases bao phủ toàn bộ luồng từ auth → report.

---

> 📝 **Ghi chú:** Khi Swagger đã tích hợp, tất cả test cases trên có thể thực thi trực tiếp tại `http://localhost:8080/swagger-ui/index.html` mà không cần công cụ thêm.
