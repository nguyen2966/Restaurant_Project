Chạy lệnh này để nạp biến từ .env và chạy app

```
Get-Content .env | ForEach-Object { if ($_ -match "^(.*?)=(.*)$") { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }; ./mvnw spring-boot:run
```

# Hệ Thống Quản Lý Nhà Hàng

> **Restaurant Management System** — Spring Boot 4.0.5· PostgreSQL · JWT Security

---

## Mục Lục

1. [Tổng Quan](#1-tổng-quan)
2. [Kiến Trúc Hệ Thống](#2-kiến-trúc-hệ-thống)
3. [Cấu Trúc Dự Án](#3-cấu-trúc-dự-án)
4. [Các Domain](#4-các-domain)
5. [Orchestration — Facade Layer](#5-orchestration--facade-layer)
6. [Luồng Sự Kiện (Event Flow)](#6-luồng-sự-kiện-event-flow)
7. [Shared Layer](#7-shared-layer)
8. [Phân Quyền Người Dùng](#8-phân-quyền-người-dùng)
9. [Tài Liệu API](#9-tài-liệu-api)
10. [Hướng Dẫn Test API với Swagger](#10-hướng-dẫn-test-api-với-swagger)
11. [Cấu Hình & Khởi Chạy](#11-cấu-hình--khởi-chạy)
12. [Database & Migration](#12-database--migration)

---

## 1. Tổng Quan

Hệ thống quản lý nhà hàng tập trung, xây dựng theo kiến trúc **Domain-Driven Design (DDD)** kết hợp **Facade Pattern** để điều phối giữa các domain. Hệ thống hỗ trợ toàn bộ vòng đời phục vụ khách hàng:

| Giai đoạn | Chức năng |
|-----------|-----------|
| **Đón khách** | Quản lý bàn, đặt bàn trước, hàng đợi walk-in |
| **Gọi món** | Xem menu, tạo order, note đặc biệt, cảnh báo dị ứng |
| **Nhà bếp** | Queue ticket theo station, State Machine trạng thái nấu |
| **Thanh toán** | Tính bill + khuyến mãi + thuế, split bill, hoàn tiền |
| **Kho** | Tự động trừ kho khi món xong, cảnh báo tồn kho thấp |
| **Báo cáo** | Doanh thu, SLA bếp, hiệu suất bàn — xuất file |

### Tech Stack

| Thành phần | Công nghệ |
|------------|-----------|
| Framework | Spring Boot 4.0.5 (Java 21) |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| JSONB | Hypersistence Utils 3.7.3 |
| API Docs | Swagger / SpringDoc OpenAPI |
| Build | Maven |

---

## 2. Kiến Trúc Hệ Thống

```
┌──────────────────────────────────────────────────────┐
│                   CLIENT (POS / Browser)              │
└───────────────────────┬──────────────────────────────┘
                        │ HTTP + JWT
┌───────────────────────▼──────────────────────────────┐
│              CONTROLLER LAYER (REST)                  │
│  OrderController  KitchenController  PaymentController│
│  MenuController   SeatingController  InventoryCtrl    │
│  PromoController  ReportController                    │
└──────┬────────────────────────────────────┬──────────┘
       │ trực tiếp                          │ qua Facade
┌──────▼──────────┐              ┌──────────▼──────────┐
│  DOMAIN SERVICES│              │  ORCHESTRATION       │
│  OrderService   │              │  OrderingFacade      │
│  MenuService    │◄─────────────│  AdminCatalogFacade  │
│  KitchenService │              │  CheckoutFacade      │
│  PromoService   │              │  ReportingDataFacade │
│  InventoryService│             └─────────────────────┘
│  SeatingService │
│  PaymentService │
└──────┬──────────┘
       │
┌──────▼──────────┐
│  REPOSITORY     │◄──── Spring Data JPA
│  (JpaRepository)│
└──────┬──────────┘
       │
┌──────▼──────────┐
│   PostgreSQL    │
└─────────────────┘

EVENT BUS (Spring ApplicationEventPublisher)
  OrderPlacedEvent ──────► KitchenService (tạo tickets)
  KitchenItemDoneEvent ──► InventoryService (trừ kho)
  PaymentCompletedEvent ──► OrderService (PAID) + SeatingService (dọn bàn)
  OrderPaidEvent ────────► SeatingService (clear table)
```

---

## 3. Cấu Trúc Dự Án

```
src/main/java/softarch/restaurant/
│
├── RestaurantApplication.java          # Entry point @SpringBootApplication
│
├── config/
│   ├── AsyncConfig.java                # ThreadPoolTaskExecutor cho @Async events
│   ├── JacksonConfig.java              # ObjectMapper + JavaTimeModule (ISO-8601)
│   ├── JpaConfig.java                  # @EnableJpaRepositories, @EnableTransactionManagement
│   └── SecurityConfig.java            # JWT filter chain, role-based route protection
│
├── shared/
│   ├── dto/
│   │   ├── ApiResponse.java            # Generic wrapper {success, data, message, timestamp}
│   │   └── PageResponse.java           # Pagination envelope
│   ├── event/
│   │   └── DomainEvent.java            # Base class: correlationId + occurredAt
│   ├── exception/
│   │   ├── RestaurantException.java    # Domain exception với HTTP status
│   │   └── GlobalExceptionHandler.java # @RestControllerAdvice → ApiResponse.error()
│   └── security/
│       ├── JwtService.java             # Generate/validate JWT, extract claims
│       ├── JwtAuthFilter.java          # OncePerRequestFilter — populate SecurityContext
│       └── UserDetailsServiceImpl.java # Load user+roles từ DB cho Spring Security
│
├── orchestration/                      # ← FACADE LAYER (điều phối cross-domain)
│   ├── OrderingFacade.java
│   ├── AdminCatalogFacade.java
│   ├── CheckoutFacade.java
│   └── ReportingDataFacade.java
│
└── domain/
    ├── order/          # Quản lý đơn hàng
    ├── menu/           # Thực đơn
    ├── kitchen/        # Nhà bếp (State Machine)
    ├── inventory/      # Kho nguyên liệu
    ├── payment/        # Thanh toán
    ├── promotion/      # Khuyến mãi
    ├── seating/        # Bàn ghế & đặt bàn
    └── report/         # Báo cáo & analytics (Strategy Pattern)
```

Mỗi domain tuân theo cấu trúc lớp nhất quán:

```
domain/{name}/
├── controller/     # REST endpoints — nhận request, trả response
├── service/        # Interface + Impl — business logic
├── repository/     # JpaRepository — truy vấn DB
├── entity/         # @Entity JPA + Enum
├── event/          # Domain events được publish
├── listener/       # Xử lý events từ domain khác
└── dto/            # Request/Response records
```

---

## 4. Các Domain

### 4.1 Order Domain

**Mục đích:** Quản lý vòng đời đơn hàng từ khi tạo đến khi thanh toán xong.

**State Machine:**
```
DRAFT ──► PLACED ──► PAID
  │                    
  └──────────────► CANCELLED
```

| Class | Vai trò |
|-------|---------|
| `Order` | Aggregate root — chứa danh sách `OrderItem`, tính `subTotal` |
| `OrderItem` | Snapshot giá tại thời điểm order, special notes, options (JSONB) |
| `OrderServiceImpl` | Persist order, publish `OrderPlacedEvent` + `OrderPaidEvent` |
| `OrderRepository` | Query theo tableId, status, khoảng thời gian |
| `OrderItemRepository` | `existsInActiveOrder()` — dùng bởi MenuService validation |
| `PaymentCompletedListener` | Nhận `PaymentCompletedEvent` → gọi `markAsPaid()` |
| `SaleData` | DTO aggregate doanh thu per menu item — dùng bởi ReportingDataFacade |

**Key rule:** `OrderController` **không được** gọi `OrderService.placeOrder()` trực tiếp. Mọi order mới phải đi qua `OrderingFacade`.

---

### 4.2 Menu Domain

**Mục đích:** Quản lý thực đơn — CRUD món ăn, kiểm soát trạng thái, validate trước khi order/promo.

**ItemStatus:**
```
ACTIVE ──► INACTIVE
  │
  └──► OUT_OF_STOCK
```

| Class | Vai trò |
|-------|---------|
| `MenuItem` | Entity — name, basePrice, description, allergens (JSONB), status |
| `MenuServiceImpl` | CRUD + `validateItemsActive()` + `validateBeforeDisable()` |
| `MenuRepository` | `findActiveByIds()` — batch validation cho OrderingFacade |
| `MenuController` | DELETE/PATCH status → route qua `AdminCatalogFacade` để guard |

**Circular dependency giải pháp:** `MenuServiceImpl` inject `PromoService` với `@Lazy` để tránh vòng lặp bean.

---

### 4.3 Kitchen Domain — State Pattern

**Mục đích:** Quản lý hàng đợi bếp với State Machine cho mỗi ticket.

**Trạng thái KitchenTicket:**
```
QUEUED ──► COOKING ──► READY ──► DELIVERED
             │  ▲
             ▼  │
           PAUSED
```

Mỗi trạng thái là một class riêng implement `TicketState`:

| State Class | Transition được phép |
|-------------|----------------------|
| `QueuedState` | `startCooking()` |
| `CookingState` | `markDone()`, `pause()`, `undo()` |
| `ReadyState` | `deliver()`, `undo()` |
| `PausedState` | `startCooking()` (resume), `undo()` |
| `DeliveredState` | Terminal — không có transition |

**Persistence:** `statusEnumValue` (String) lưu xuống DB, `@PostLoad` restore lại state object.

| Class | Vai trò |
|-------|---------|
| `KitchenServiceImpl` | Tạo tickets từ event, điều hướng state transitions |
| `OrderPlacedListener` | Nhận `OrderPlacedEvent` → tạo `KitchenTicket` per `OrderItem` |
| `KitchenItemDoneEvent` | Publish khi ticket → READY → Inventory auto-deduct |
| `KitchenTicketFilter` | DTO filter theo station, status, nearDeadline, sortBy |

---

### 4.4 Inventory Domain

**Mục đích:** Theo dõi tồn kho nguyên liệu, tự động trừ theo recipe, cảnh báo thấp.

| Class | Vai trò |
|-------|---------|
| `Ingredient` | Entity — currentStock, minThreshold, unit. Method `isLowStock()`, `deduct()`, `restock()` |
| `RecipeItem` | Mapping menuItemId → ingredientId + requiredAmount |
| `InventoryTransaction` | Audit log mọi thay đổi tồn kho (reason: PREP/SPOILAGE/AUTO_DEDUCT/RESTOCK) |
| `Reorder` | Gợi ý đặt hàng từ forecast algorithm |
| `InventoryServiceImpl` | `checkAvailability()` aggregate demand cross-item trước order |
| `KitchenItemDoneListener` | `@Async` — auto-deduct theo recipe khi ticket DONE |

**checkAvailability logic:**
1. Load recipe của từng menu item trong order
2. Aggregate total demand per ingredient (2 món Phở + 1 Bún chung beef)
3. So sánh với `currentStock` — trả về danh sách shortfalls nếu thiếu

---

### 4.5 Payment Domain

**Mục đích:** Xử lý thanh toán, hoàn tiền, split bill.

| Class | Vai trò |
|-------|---------|
| `PaymentTransaction` | Entity — amount, tip, tax, discount, method, status, gatewayRefId |
| `PaymentServiceImpl` | processPayment → guard duplicate → markCompleted → publish `PaymentCompletedEvent` |
| `PaymentController` | Nhận payment request → validate qua `CheckoutFacade` trước khi charge |

**PaymentMethod:** `CASH`, `CREDIT_CARD`, `ONLINE_BANKING`, `E_WALLET`

**PaymentStatus:** `PENDING` → `COMPLETED` | `FAILED` | `REFUNDED`

---

### 4.6 Promotion Domain

**Mục đích:** Quản lý chương trình khuyến mãi và tính discount.

**PromoType & Logic:**

| PromoType | Công thức tính discount |
|-----------|------------------------|
| `BY_PERCENT` | `subTotal × (discountValue / 100)` |
| `BY_AMOUNT` | `discountValue` (cố định, không vượt subTotal) |
| `COMBO` | Áp dụng nếu TẤT CẢ items trong combo có mặt trong order |
| `BUY_X_GET_Y` | Parse `condition` lấy X, tính số sets = quantity / (X+1) × discountValue |

| Class | Vai trò |
|-------|---------|
| `PromoItem` | Entity — menuItemIds (ElementCollection), startDate/dueDate, discountValue |
| `PromoServiceImpl` | `calculateDiscount()` iterate tất cả ACTIVE promos, cộng dồn discount |
| `PromoRepository` | `findActiveByMenuItemId()` — kiểm tra item có đang trong promo không |

---

### 4.7 Seating Domain

**Mục đích:** Quản lý bàn ghế, đặt bàn, hàng đợi walk-in.

**TableStatus:** `AVAILABLE → SEATED → ORDERING → SERVING → CHECK_REQUESTED → DIRTY → LOCKED`

| Class | Vai trò |
|-------|---------|
| `RestaurantTable` | Entity — tableCode, capacity, status. Methods: `markAsSeated()`, `markAsDirty()` |
| `Reservation` | Entity — BOOKED → SEATED / NO_SHOW / CANCELLED |
| `WaitlistEntry` | Entity — joinedAt, isNotified |
| `SeatingServiceImpl` | Auto-assign bàn nếu không chỉ định, move/merge tables |
| `OrderPaymentListener` | Nhận `OrderPaidEvent` → `clearTable()` → DIRTY |

---

### 4.8 Report Domain — Strategy Pattern

**Mục đích:** Sinh báo cáo đa loại — doanh thu, SLA bếp, hiệu suất bàn.

**ReportType:** `SALE`, `MENU`, `SLA`, `TABLE`

| Strategy Class | Xử lý loại báo cáo |
|---------------|---------------------|
| `SalesReportStrategy` | SALE — tổng doanh thu, orders, avg order value |
| `MenuReportStrategy` | MENU — top 10 best sellers theo quantity |
| `SLAReportStrategy` | SLA — % breach rate, avg completion time per món |
| `TableReportStrategy` | TABLE — sessions per bàn, avg session time |

**Flow:** `ReportController` → `ReportServiceImpl.generateReport()` → iterate `List<ReportStrategy>` → `strategy.isSupported(type)` → `strategy.execute(req, dataFacade)` → `ReportingDataFacade.fetch*()` → domain services.

---

## 5. Orchestration — Facade Layer

Facade layer nằm ở `softarch.restaurant.orchestration/` — điều phối các use case yêu cầu nhiều domain phối hợp.

### 5.1 OrderingFacade

**Use case:** UC-01 — Tạo order mới

**Dependencies:** `OrderService` + `MenuService` + `InventoryService`

```
POST /api/orders
        │
        ▼
OrderingFacade.validateAndPlaceOrder()
        │
        ├─ 1. MenuService.validateItemsActive(menuItemIds)
        │      └─ Ném BadRequest nếu item INACTIVE/OUT_OF_STOCK/không tồn tại
        │
        ├─ 2. InventoryService.checkAvailability(menuItemQuantities)
        │      └─ Aggregate demand, so sánh stock → ném Unprocessable nếu thiếu
        │
        ├─ 3. Xây dựng Order aggregate
        │      └─ Snapshot giá, detect allergy alert, gắn options
        │
        └─ 4. OrderService.placeOrder(order)
               └─ DRAFT → PLACED → publish OrderPlacedEvent
```

**Quan trọng:** `OrderController` **bắt buộc** phải dùng facade. Không được bypass để gọi `OrderService` trực tiếp khi tạo order mới.

---

### 5.2 AdminCatalogFacade

**Use case:** UC-04, UC-05 — Quản lý Menu & Promo cho Admin

**Dependencies:** `MenuService` + `PromoService` + `OrderService`

```
disableMenuItem(menuItemId)
        │
        ├─ Guard 1: OrderService.isItemInActiveOrder(id)
        │    └─ Conflict nếu item đang trong order chưa hoàn thành
        │
        ├─ Guard 2: PromoService.isItemInActivePromo(id)
        │    └─ Conflict nếu item đang trong promotion đang chạy
        │
        └─ MenuService.setStatus(id, INACTIVE)

createPromotion(PromoRequest)
        │
        ├─ MenuService.validateItemsActive(req.menuItemIds)
        │    └─ Đảm bảo tất cả items trong promo đang ACTIVE
        │
        └─ PromoService.createItem(req)
```

`MenuController.delete()` và `MenuController.setStatus(INACTIVE)` đều route qua facade này.

---

### 5.3 CheckoutFacade

**Use case:** UC-12 — Tính bill và xử lý thanh toán

**Dependencies:** `OrderService` + `PromoService`

```
calculateOrderTotal(orderId) → OrderBillingDTO
        │
        ├─ Load Order → lấy subTotal + basket quantities
        ├─ PromoService.calculateDiscount(quantities, subTotal)
        ├─ Áp dụng VAT 10% trên sau-discount
        └─ Trả về {subTotal, discountAmount, taxAmount, total}

processCheckout(CheckoutRequest) → OrderBillingDTO
        │
        ├─ calculateOrderTotal() — validate total > 0
        └─ Return billing (PaymentController gọi PaymentService riêng)
```

---

### 5.4 ReportingDataFacade

**Use case:** UC-14 — Tổng hợp dữ liệu báo cáo

**Dependencies:** `OrderService` + `KitchenService` + `SeatingService`

```
fetchSalesData()     → OrderService.getSalesData()
fetchKitchenSLA()    → KitchenService.getSLAData()
fetchTableTurnover() → SeatingService.getTableTurnover()
```

Facade này là cầu nối để các `ReportStrategy` truy cập dữ liệu từ nhiều bounded context mà không cần import trực tiếp các domain service vào analytics layer.

---

## 6. Luồng Sự Kiện (Event Flow)

Các domain giao tiếp qua Spring `ApplicationEventPublisher` — không gọi service của nhau trực tiếp.

```
[Staff tạo order]
      │
      ▼
OrderServiceImpl.placeOrder()
      └─► publish OrderPlacedEvent
                    │
                    ▼
          OrderPlacedListener (kitchen domain)
                    └─► KitchenService.createTicketsForOrder()
                              └─ Tạo KitchenTicket per OrderItem (QUEUED)

[Chef bấm "Done"]
      │
      ▼
KitchenServiceImpl.processMarkDone()
      └─► publish KitchenItemDoneEvent  (@Async)
                    │
                    ▼
          KitchenItemDoneListener (inventory domain)
                    └─► InventoryService.autoDeductForMenuItem()
                              └─ Trừ kho theo recipe, ghi InventoryTransaction

[Cashier thanh toán]
      │
      ▼
PaymentServiceImpl.processPayment()
      └─► publish PaymentCompletedEvent
                    │
          ┌─────────┴──────────┐
          ▼                    ▼
PaymentCompletedListener   OrderPaymentListener
(order domain)             (seating domain)
    └─► OrderService            └─► SeatingService
         .markAsPaid()               .clearTable()
              └─► publish            └─ Table → DIRTY
               OrderPaidEvent
```

**Lưu ý:** `KitchenItemDoneListener` chạy `@Async` — lỗi inventory deduction không rollback kitchen ticket. Được log WARNING để xử lý thủ công.

---

## 7. Shared Layer

### ApiResponse\<T\>

Mọi API endpoint đều wrap response trong `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": "Order placed",
  "timestamp": "2025-04-01T10:30:00"
}
```

### RestaurantException

Các factory method chuẩn:

| Method | HTTP Status | Dùng khi |
|--------|-------------|----------|
| `notFound(resource, id)` | 404 | Entity không tìm thấy |
| `conflict(message)` | 409 | Vi phạm business rule (item đang dùng) |
| `badRequest(message)` | 400 | Input không hợp lệ |
| `unprocessable(message)` | 422 | Logic không thực hiện được (hết hàng) |
| `forbidden(message)` | 403 | Không có quyền |

### JWT Security

Token structure:

```json
{
  "sub": "server_1",
  "userId": 2,
  "role": "SERVER",
  "iat": 1712000000,
  "exp": 1712086400
}
```

---

## 8. Phân Quyền Người Dùng

| Role | Quyền truy cập |
|------|---------------|
| **MANAGER** | Toàn quyền tất cả endpoints |
| **SERVER** | Order, Menu (read), Seating, Inventory |
| **CHEF** | Kitchen queue + ticket updates, Menu (read) |
| **CASHIER** | Payment, Order (read) |

Chi tiết route protection trong `SecurityConfig.java`:

| Endpoint Pattern | Role yêu cầu |
|-----------------|--------------|
| `GET /api/menu/**` | Tất cả authenticated |
| `POST/PUT/DELETE/PATCH /api/menu/**` | MANAGER |
| `/api/orders/**` | SERVER, MANAGER |
| `/api/kitchen/**` | CHEF, MANAGER |
| `/api/payments/**` | CASHIER, MANAGER |
| `/api/promotions/**` | MANAGER |
| `/api/inventory/**` | SERVER, CHEF, MANAGER |
| `/api/seating/**` | SERVER, MANAGER |
| `/api/reports/**` | MANAGER |

---

## 9. Tài Liệu API

### 9.1 Menu API — `/api/menu`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `GET` | `/api/menu` | Xem menu (filter `?query=` & `?status=`) | Tất cả |
| `GET` | `/api/menu/{id}` | Chi tiết một món | Tất cả |
| `GET` | `/api/menu/best-sellers` | Top 10 món bán chạy | Tất cả |
| `POST` | `/api/menu` | Thêm món mới | MANAGER |
| `PUT` | `/api/menu/{id}` | Cập nhật thông tin món | MANAGER |
| `DELETE` | `/api/menu/{id}` | Vô hiệu hóa món (soft delete, → INACTIVE) | MANAGER |
| `PATCH` | `/api/menu/{id}/status` | Đổi trạng thái ACTIVE/INACTIVE/OUT_OF_STOCK | MANAGER |

**Ví dụ request — Thêm món mới:**
```json
POST /api/menu
{
  "name": "Phở Bò Đặc Biệt",
  "basePrice": 85000,
  "description": "Phở bò truyền thống với nước dùng 12 giờ",
  "allergens": ["gluten"]
}
```

---

### 9.2 Order API — `/api/orders`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `POST` | `/api/orders` | Tạo order mới (qua OrderingFacade) | SERVER, MANAGER |
| `GET` | `/api/orders/{id}` | Chi tiết order | SERVER, MANAGER |
| `GET` | `/api/orders?tableId={id}` | Tất cả orders của một bàn | SERVER, MANAGER |
| `PATCH` | `/api/orders/{orderId}/items/{itemId}/note` | Thêm ghi chú cho món | SERVER, MANAGER |
| `DELETE` | `/api/orders/{id}` | Hủy order | SERVER, MANAGER |

**Ví dụ request — Tạo order:**
```json
POST /api/orders
Headers: X-User-Id: 2
{
  "tableId": 3,
  "type": "DINE_IN",
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "specialNotes": "Không hành, ít đường",
      "options": { "spice": "mild" }
    },
    {
      "menuItemId": 8,
      "quantity": 1,
      "specialNotes": null,
      "options": {}
    }
  ]
}
```

**Ví dụ response:**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "orderCode": "ORD-1712001234567",
    "tableId": 3,
    "type": "DINE_IN",
    "status": "PLACED",
    "subTotal": 170000,
    "createdAt": "2025-04-01T10:30:00",
    "items": [
      {
        "id": 101,
        "menuItemId": 1,
        "quantity": 2,
        "priceAtPurchase": 75000,
        "lineTotal": 150000,
        "specialNotes": "Không hành, ít đường",
        "isAllergyAlert": false
      }
    ]
  },
  "message": "Order placed"
}
```

---

### 9.3 Kitchen API — `/api/kitchen`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `GET` | `/api/kitchen/queue` | Xem hàng đợi bếp (filter theo station, status, nearDeadline) | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/start` | Bắt đầu nấu (QUEUED → COOKING) | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/done` | Hoàn thành (COOKING → READY) | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/pause` | Tạm dừng (COOKING → PAUSED) | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/undo` | Hoàn tác về trạng thái trước | CHEF, MANAGER |
| `GET` | `/api/kitchen/sla?from={dt}&to={dt}` | Báo cáo SLA theo thời gian | MANAGER |

**Ví dụ filter queue:**
```
GET /api/kitchen/queue?stations=GRILL,COLD&status=QUEUED&nearDeadline=true&sortBy=deadline
```

---

### 9.4 Payment API — `/api/payments`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `GET` | `/api/payments/orders/{orderId}/total` | Xem tổng bill (subTotal + discount + thuế) | CASHIER, MANAGER |
| `POST` | `/api/payments` | Thanh toán order | CASHIER, MANAGER |
| `POST` | `/api/payments/{id}/refund` | Hoàn tiền | CASHIER, MANAGER |
| `GET` | `/api/payments/orders/{orderId}` | Lịch sử thanh toán của order | CASHIER, MANAGER |

**Ví dụ — Thanh toán:**
```
POST /api/payments?orderId=42&amount=187000&method=CASH&tip=20000
```

**Response billing:**
```json
{
  "success": true,
  "data": {
    "orderId": 42,
    "orderCode": "ORD-1712001234567",
    "subTotal": 170000,
    "discountAmount": 0,
    "taxAmount": 17000,
    "total": 187000
  }
}
```

---

### 9.5 Promotion API — `/api/promotions`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `GET` | `/api/promotions?status=ACTIVE` | Danh sách khuyến mãi | MANAGER |
| `POST` | `/api/promotions` | Tạo chương trình khuyến mãi | MANAGER |
| `PUT` | `/api/promotions/{id}` | Cập nhật khuyến mãi | MANAGER |
| `DELETE` | `/api/promotions/{id}` | Vô hiệu hóa khuyến mãi | MANAGER |
| `PATCH` | `/api/promotions/{id}/status` | Đổi trạng thái | MANAGER |
| `POST` | `/api/promotions/simulate` | Preview discount cho một basket | MANAGER |

**Ví dụ — Tạo promo giảm 30%:**
```json
POST /api/promotions
{
  "name": "Happy Hour Drinks",
  "promoType": "BY_PERCENT",
  "condition": "Áp dụng 14:00-17:00",
  "menuItemIds": [7, 8, 9],
  "startDate": "2025-04-01T14:00:00",
  "dueDate": "2025-04-30T17:00:00",
  "discountValue": 30
}
```

**Ví dụ — Simulate:**
```json
POST /api/promotions/simulate
{
  "menuItemQuantities": { "7": 2, "8": 1 },
  "subTotal": 125000
}
```

---

### 9.6 Inventory API — `/api/inventory`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `POST` | `/api/inventory/usage` | Nhập thủ công lượng nguyên liệu đã dùng | SERVER, CHEF, MANAGER |
| `GET` | `/api/inventory/alerts` | Danh sách nguyên liệu dưới ngưỡng tối thiểu | Tất cả |

**Ví dụ — Nhập thủ công:**
```json
POST /api/inventory/usage
Headers: X-User-Id: 3
[
  { "ingredientId": 1, "amount": 2.5, "reason": "SPOILAGE" },
  { "ingredientId": 5, "amount": 1.0, "reason": "ADJUSTMENT" }
]
```

---

### 9.7 Seating API — `/api/seating`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `GET` | `/api/seating/tables` | Toàn bộ sơ đồ bàn + trạng thái | SERVER, MANAGER |
| `POST` | `/api/seating/tables/{id}/seat-walkin` | Nhận khách walk-in vào bàn | SERVER, MANAGER |
| `POST` | `/api/seating/reservations` | Đặt bàn trước | SERVER, MANAGER |
| `POST` | `/api/seating/reservations/{id}/seat` | Check-in khách đã đặt bàn | SERVER, MANAGER |
| `POST` | `/api/seating/reservations/{id}/no-show` | Đánh dấu no-show | SERVER, MANAGER |
| `GET` | `/api/seating/waitlist` | Xem hàng đợi walk-in | SERVER, MANAGER |
| `POST` | `/api/seating/waitlist` | Thêm khách vào waitlist | SERVER, MANAGER |
| `POST` | `/api/seating/tables/move?from={}&to={}` | Chuyển bàn | SERVER, MANAGER |
| `POST` | `/api/seating/tables/merge` | Gộp bàn | SERVER, MANAGER |

**Ví dụ — Đặt bàn:**
```json
POST /api/seating/reservations
{
  "customerName": "Nguyễn Văn An",
  "customerPhone": "0912345678",
  "partySize": 4,
  "reservedTime": "2025-04-01T19:00:00"
}
```

---

### 9.8 Report API — `/api/reports`

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| `POST` | `/api/reports/dashboard` | Sinh dữ liệu báo cáo để hiển thị | MANAGER |
| `POST` | `/api/reports/export` | Sinh báo cáo + lưu file, trả URL download | MANAGER |

**ReportType:** `SALE` · `MENU` · `SLA` · `TABLE`

**Ví dụ — Báo cáo doanh thu:**
```json
POST /api/reports/dashboard
{
  "type": "SALE",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "summary": "Sales report: 45,200,000 VND revenue across 312 orders",
    "dataPoints": {
      "totalRevenue": 45200000,
      "totalOrders": 312,
      "averageOrderValue": 144871,
      "breakdown": [...]
    }
  }
}
```

---

## 10. Hướng Dẫn Test API với Swagger

### 10.1 Truy cập Swagger UI

Sau khi khởi động ứng dụng, mở trình duyệt và vào:

```
http://localhost:8080/swagger-ui/index.html
```

Hoặc xem raw OpenAPI spec tại:

```
http://localhost:8080/v3/api-docs
```

---

### 10.2 Bước 1 — Lấy JWT Token

Vì API được bảo vệ bằng JWT, bạn cần authenticate trước khi test bất kỳ endpoint nào.

> **Lưu ý:** Nếu chưa có endpoint `/api/auth/login`, hãy tạm thời disable security trong dev bằng cách comment `@EnableWebSecurity` trong `SecurityConfig.java`, hoặc thêm endpoint login vào project.

**Bước thực hiện trong Swagger:**

1. Tìm section **Auth** trong Swagger UI
2. Gọi `POST /api/auth/login` với body:
   ```json
   {
     "username": "admin",
     "password": "password123"
   }
   ```
3. Copy giá trị `token` từ response

**Ví dụ response login:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJNQU5BR0VSIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3MTIwMDAwMDAsImV4cCI6MTcxMjA4NjQwMH0.xxxx",
    "username": "admin",
    "role": "MANAGER"
  }
}
```

---

### 10.3 Bước 2 — Authorize trong Swagger

1. Click nút **"Authorize"** 🔒 ở góc trên bên phải Swagger UI
2. Trong ô **`bearerAuth`**, nhập:
   ```
   Bearer eyJhbGciOiJIUzI1NiJ9...
   ```
   *(bao gồm từ `Bearer ` phía trước)*
3. Click **"Authorize"** → **"Close"**

Từ đây tất cả request từ Swagger sẽ tự động gắn JWT header.

---

### 10.4 Bước 3 — Test theo kịch bản thực tế

Thực hiện theo thứ tự sau để test đầy đủ luồng:

#### Kịch bản: Khách đến ăn, gọi món, thanh toán

```
① Xem sơ đồ bàn
   GET /api/seating/tables
   → Tìm bàn AVAILABLE

② Nhận khách vào bàn
   POST /api/seating/tables/{id}/seat-walkin
   Params: partySize=2

③ Xem menu
   GET /api/menu?status=ACTIVE

④ Tạo order
   POST /api/orders
   Header: X-User-Id: 2
   Body: { tableId, type: "DINE_IN", items: [...] }
   → Nhận orderCode

⑤ [Bếp] Xem queue
   GET /api/kitchen/queue

⑥ [Bếp] Bắt đầu nấu
   PATCH /api/kitchen/tickets/{id}/start

⑦ [Bếp] Hoàn thành
   PATCH /api/kitchen/tickets/{id}/done
   → KitchenItemDoneEvent → auto-deduct inventory

⑧ Xem bill
   GET /api/payments/orders/{orderId}/total
   → Thấy subTotal + discount + tax + total

⑨ Thanh toán
   POST /api/payments
   Params: orderId, amount, method=CASH, tip=20000
   → PaymentCompletedEvent → Order PAID → Table DIRTY
```

---

### 10.5 Test Từng Domain

#### Menu

```bash
# Xem tất cả món đang active
GET /api/menu?status=ACTIVE

# Tìm kiếm theo tên
GET /api/menu?query=phở

# Thêm món (cần MANAGER token)
POST /api/menu
Body: { "name": "Bún Chả", "basePrice": 65000, "allergens": [] }

# Đổi trạng thái
PATCH /api/menu/{id}/status
Body: { "status": "OUT_OF_STOCK" }
```

#### Promotion

```bash
# Tạo promo giảm 20% cho drinks
POST /api/promotions
Body: {
  "name": "Weekend Special",
  "promoType": "BY_PERCENT",
  "menuItemIds": [7, 8, 9],
  "startDate": "2025-04-05T00:00:00",
  "dueDate": "2025-04-06T23:59:59",
  "discountValue": 20
}

# Simulate xem discount bao nhiêu
POST /api/promotions/simulate
Body: {
  "menuItemQuantities": {"7": 2, "8": 1},
  "subTotal": 125000
}
```

#### Inventory

```bash
# Xem cảnh báo tồn kho thấp
GET /api/inventory/alerts

# Nhập thủ công hao hụt
POST /api/inventory/usage
Body: [{ "ingredientId": 1, "amount": 0.5, "reason": "SPOILAGE" }]
```

#### Report

```bash
# Báo cáo doanh thu tháng 4
POST /api/reports/dashboard
Body: {
  "type": "SALE",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}

# Xuất báo cáo SLA bếp
POST /api/reports/export
Body: {
  "type": "SLA",
  "startDate": "2025-04-01T00:00:00",
  "dueDate": "2025-04-30T23:59:59"
}
```

---

### 10.6 Mã Lỗi Thường Gặp

| HTTP Status | ErrorCode | Nguyên nhân phổ biến |
|------------|-----------|----------------------|
| `400 Bad Request` | `BAD_REQUEST` | Validation lỗi, thiếu field bắt buộc, item không ACTIVE |
| `401 Unauthorized` | — | Thiếu hoặc JWT hết hạn |
| `403 Forbidden` | `FORBIDDEN` | Role không có quyền truy cập endpoint |
| `404 Not Found` | `NOT_FOUND` | Entity không tồn tại (Order, MenuItem...) |
| `409 Conflict` | `CONFLICT` | Vi phạm business rule (item đang dùng, order đã paid) |
| `422 Unprocessable` | `UNPROCESSABLE` | Logic không thể thực hiện (hết stock, order total = 0) |
| `500 Internal Server Error` | — | Lỗi hệ thống — kiểm tra log |

**Ví dụ error response:**
```json
{
  "success": false,
  "data": null,
  "message": "[CONFLICT] Cannot disable 'Phở Bò': it is part of an active order.",
  "timestamp": "2025-04-01T10:30:00"
}
```

---

## 11. Cấu Hình & Khởi Chạy

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

### Biến môi trường

| Biến | Mô tả | Default (dev) |
|------|-------|---------------|
| `DB_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/restaurant_db` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASS` | Database password | `postgres` |
| `jwt.secret` | Secret key cho JWT signing (≥256 bit) | Hardcoded dev key |
| `jwt.expiration-ms` | Thời gian hết hạn token (ms) | `86400000` (24h) |

### Khởi chạy development

```bash
# Clone & build
git clone <repository>
cd restaurant

# Tạo database
psql -U postgres -c "CREATE DATABASE restaurant_dev;"

# Chạy với profile dev
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

Flyway sẽ tự động chạy migration khi khởi động.

### Profiles

| Profile | DDL | SQL Logging | Database |
|---------|-----|-------------|----------|
| `dev` | `validate` | DEBUG | `restaurant_dev` |
| `prod` | `validate` | WARN | Từ env vars |

---

## 12. Database & Migration

### Flyway Migration

Đặt migration files vào:
```
src/main/resources/db/migration/
├── V1__init_schema.sql      ← DDL toàn bộ schema
└── V2__seed_data.sql        ← Seed roles, permissions, menu items mẫu
```

File `schema.sql` đã được tạo sẵn trong thư mục gốc project — đổi tên thành `V1__init_schema.sql` và copy vào đúng vị trí.

### Entities JSONB

Hai trường dùng PostgreSQL JSONB (cần `hypersistence-utils`):

| Entity | Trường | Ví dụ |
|--------|--------|-------|
| `MenuItem` | `allergens` | `["gluten", "nuts"]` |
| `OrderItem` | `options` | `{"size": "large", "spice": "mild"}` |

### Sơ đồ quan hệ chính

```
users ──── user_roles ──── roles ──── role_permissions ──── permissions
  │
  ├── employee_profile
  └── audit_log

menu_item ──── promo_menu_mapping ──── promo_item
  │
  ├── recipe_item ──── ingredient
  └── order_item ──── orders ──── restaurant_table
                          │
                          ├── kitchen_ticket
                          └── payment_transaction
```

---

> **Tác giả:** SoftArch Restaurant Team  
> **Cập nhật lần cuối:** Tháng 4, 2025  
> **License:** Proprietary
