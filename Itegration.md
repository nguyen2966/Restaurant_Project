# Tài Liệu Tích Hợp API & Kiến Trúc Frontend

> **Backend:** Spring Boot 3.3 · `http://localhost:8080`
> **Frontend stack đề xuất:** React 18 · TypeScript · TanStack Query · Zustand · Tailwind CSS

---

## Mục Lục

1. [Kiến trúc Frontend đề xuất](#1-kiến-trúc-frontend-đề-xuất)
2. [Cấu hình API Client](#2-cấu-hình-api-client)
3. [Authentication](#3-authentication)
4. [Menu API](#4-menu-api)
5. [Order API](#5-order-api)
6. [Kitchen API (+ Station)](#6-kitchen-api--station)
7. [Payment API](#7-payment-api)
8. [Promotion API](#8-promotion-api)
9. [Inventory API](#9-inventory-api)
10. [Seating API](#10-seating-api)
11. [Report API](#11-report-api)
12. [Error Handling toàn cục](#12-error-handling-toàn-cục)
13. [React Query patterns](#13-react-query-patterns)
14. [Hướng dẫn tổ chức Zustand stores](#14-hướng-dẫn-tổ-chức-zustand-stores)

---

## 1. Kiến trúc Frontend đề xuất

### 1.1 Tổng quan

```
restaurant-frontend/
├── src/
│   ├── api/                    # Axios instances + all API calls
│   │   ├── client.ts           # Axios instance + interceptors
│   │   ├── auth.api.ts
│   │   ├── menu.api.ts
│   │   ├── order.api.ts
│   │   ├── kitchen.api.ts
│   │   ├── payment.api.ts
│   │   ├── promotion.api.ts
│   │   ├── inventory.api.ts
│   │   ├── seating.api.ts
│   │   └── report.api.ts
│   │
│   ├── hooks/                  # TanStack Query hooks (useQuery / useMutation)
│   │   ├── useMenu.ts
│   │   ├── useOrder.ts
│   │   ├── useKitchen.ts
│   │   ├── usePayment.ts
│   │   ├── usePromotion.ts
│   │   ├── useInventory.ts
│   │   ├── useSeating.ts
│   │   └── useReport.ts
│   │
│   ├── stores/                 # Zustand global state
│   │   ├── auth.store.ts       # JWT token, current user, role
│   │   ├── cart.store.ts       # Order đang build (draft items)
│   │   └── kitchen.store.ts    # Real-time queue state
│   │
│   ├── types/                  # TypeScript interfaces khớp với API response
│   │   ├── api.types.ts        # ApiResponse<T>, pagination
│   │   ├── auth.types.ts
│   │   ├── menu.types.ts
│   │   ├── order.types.ts
│   │   ├── kitchen.types.ts
│   │   ├── payment.types.ts
│   │   ├── promotion.types.ts
│   │   ├── inventory.types.ts
│   │   ├── seating.types.ts
│   │   └── report.types.ts
│   │
│   ├── pages/                  # Route-level pages (1 page = 1 role view)
│   │   ├── LoginPage.tsx
│   │   ├── server/             # SERVER role
│   │   │   ├── FloorPlanPage.tsx
│   │   │   ├── MenuPage.tsx
│   │   │   └── OrderPage.tsx
│   │   ├── kitchen/            # CHEF role
│   │   │   └── KitchenQueuePage.tsx
│   │   ├── cashier/            # CASHIER role
│   │   │   └── CheckoutPage.tsx
│   │   └── manager/            # MANAGER role
│   │       ├── DashboardPage.tsx
│   │       ├── MenuManagePage.tsx
│   │       ├── PromotionPage.tsx
│   │       ├── InventoryPage.tsx
│   │       └── ReportPage.tsx
│   │
│   ├── components/             # Shared UI components
│   │   ├── ui/                 # Button, Input, Modal, Badge...
│   │   ├── layout/             # Sidebar, Header, RoleGuard
│   │   └── domain/             # MenuCard, TicketCard, OrderSummary...
│   │
│   └── router/
│       └── index.tsx           # React Router v6 + role-based guards
```

### 1.2 Role → Route mapping

| Role | Default route | Các trang có thể truy cập |
|------|--------------|--------------------------|
| `MANAGER` | `/manager/dashboard` | Tất cả |
| `SERVER` | `/server/floor-plan` | `/server/*`, `/menu` |
| `CHEF` | `/kitchen/queue` | `/kitchen/*` |
| `CASHIER` | `/cashier/checkout` | `/cashier/*` |

### 1.3 Tech stack chi tiết

| Thư viện | Phiên bản | Mục đích |
|---------|-----------|----------|
| React | 18 | UI framework |
| TypeScript | 5 | Type safety |
| Vite | 5 | Build tool (nhanh hơn CRA) |
| React Router | 6 | Client-side routing |
| TanStack Query | 5 | Server state, caching, refetch |
| Zustand | 4 | Client state (auth, cart) |
| Axios | 1.6 | HTTP client |
| Tailwind CSS | 3 | Utility-first styling |
| shadcn/ui | latest | Component library |
| React Hot Toast | 2 | Notifications |
| date-fns | 3 | Date formatting |

---

## 2. Cấu hình API Client

### 2.1 Base Axios instance (`src/api/client.ts`)

```typescript
import axios, { AxiosError, AxiosResponse } from 'axios';
import { useAuthStore } from '../stores/auth.store';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: gắn JWT vào mọi request ──────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor: unwrap ApiResponse<T> ───────────────────────────
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // Unwrap { success, data, message } → trả về data trực tiếp
    if (response.data?.success === true) {
      return { ...response, data: response.data.data };
    }
    return response;
  },
  (error: AxiosError) => {
    const status = error.response?.status;

    // Token hết hạn → logout + redirect login
    if (status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }

    // Lấy message từ ApiResponse.error format
    const message =
      (error.response?.data as any)?.message ?? error.message;

    return Promise.reject(new AppError(message, status ?? 0));
  }
);

// Custom error class để phân biệt với network errors
export class AppError extends Error {
  constructor(
    message: string,
    public statusCode: number
  ) {
    super(message);
    this.name = 'AppError';
  }
}
```

### 2.2 TypeScript types nền (`src/types/api.types.ts`)

```typescript
// Wrapper chuẩn của mọi API response
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  timestamp: string;
}

// Sau khi interceptor unwrap, component nhận thẳng T
// Không cần access .data.data ở component

export type ID = number;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
```

---

## 3. Authentication

### Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/auth/login` | Đăng nhập, nhận JWT |
| `POST` | `/api/auth/validate` | Kiểm tra token còn hạn không |

### Request / Response

**POST `/api/auth/login`**

```typescript
// Request body
interface LoginRequest {
  username: string;
  password: string;
}

// Response data (sau khi interceptor unwrap)
interface LoginResponse {
  token: string;
  username: string;
  role: 'MANAGER' | 'SERVER' | 'CHEF' | 'CASHIER';
  expiresInSeconds: number;
}
```

**POST `/api/auth/validate`**

```
Header: Authorization: Bearer {token}
Response data: boolean  (true = còn hạn)
```

### API function (`src/api/auth.api.ts`)

```typescript
import { apiClient } from './client';
import type { LoginRequest, LoginResponse } from '../types/auth.types';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/api/auth/login', data)
      .then(res => res.data),

  validate: () =>
    apiClient.post<boolean>('/api/auth/validate')
      .then(res => res.data),
};
```

### Zustand Auth Store (`src/stores/auth.store.ts`)

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Role = 'MANAGER' | 'SERVER' | 'CHEF' | 'CASHIER';

interface AuthState {
  token: string | null;
  username: string | null;
  role: Role | null;
  userId: number | null;
  login: (token: string, username: string, role: Role) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
  hasRole: (...roles: Role[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      username: null,
      role: null,
      userId: null,

      login: (token, username, role) =>
        set({ token, username, role }),

      logout: () =>
        set({ token: null, username: null, role: null, userId: null }),

      isAuthenticated: () => !!get().token,

      hasRole: (...roles) => {
        const role = get().role;
        return role ? roles.includes(role) : false;
      },
    }),
    { name: 'restaurant-auth' } // persist to localStorage
  )
);
```

### Hook (`src/hooks/useAuth.ts`)

```typescript
import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/auth.api';
import { useAuthStore } from '../stores/auth.store';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

// Role → default redirect path
const ROLE_REDIRECT: Record<string, string> = {
  MANAGER: '/manager/dashboard',
  SERVER:  '/server/floor-plan',
  CHEF:    '/kitchen/queue',
  CASHIER: '/cashier/checkout',
};

export function useLogin() {
  const { login } = useAuthStore();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      login(data.token, data.username, data.role);
      toast.success(`Xin chào, ${data.username}!`);
      navigate(ROLE_REDIRECT[data.role] ?? '/');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 4. Menu API

### Endpoints

| Method | Path | Query params | Role |
|--------|------|-------------|------|
| `GET` | `/api/menu` | `query`, `status` | Tất cả |
| `GET` | `/api/menu/{id}` | — | Tất cả |
| `GET` | `/api/menu/best-sellers` | — | Tất cả |
| `POST` | `/api/menu` | — | MANAGER |
| `PUT` | `/api/menu/{id}` | — | MANAGER |
| `DELETE` | `/api/menu/{id}` | — | MANAGER |
| `PATCH` | `/api/menu/{id}/status` | — | MANAGER |

### Types (`src/types/menu.types.ts`)

```typescript
export type ItemStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';

export interface MenuItem {
  id: number;
  name: string;
  basePrice: number;
  description: string | null;
  allergens: string[] | null;
  status: ItemStatus;
}

export interface MenuRequest {
  name: string;
  basePrice: number;
  description?: string;
  allergens?: string[];
}

export interface StatusRequest {
  status: ItemStatus;
}
```

### API functions (`src/api/menu.api.ts`)

```typescript
import { apiClient } from './client';
import type { MenuItem, MenuRequest, StatusRequest, ItemStatus } from '../types/menu.types';

export const menuApi = {
  getAll: (params?: { query?: string; status?: ItemStatus }) =>
    apiClient.get<MenuItem[]>('/api/menu', { params }).then(r => r.data),

  getById: (id: number) =>
    apiClient.get<MenuItem>(`/api/menu/${id}`).then(r => r.data),

  getBestSellers: () =>
    apiClient.get<MenuItem[]>('/api/menu/best-sellers').then(r => r.data),

  create: (data: MenuRequest) =>
    apiClient.post<MenuItem>('/api/menu', data).then(r => r.data),

  update: (id: number, data: MenuRequest) =>
    apiClient.put<MenuItem>(`/api/menu/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    apiClient.delete<void>(`/api/menu/${id}`).then(r => r.data),

  setStatus: (id: number, data: StatusRequest) =>
    apiClient.patch<MenuItem>(`/api/menu/${id}/status`, data).then(r => r.data),
};
```

### Hooks (`src/hooks/useMenu.ts`)

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { menuApi } from '../api/menu.api';
import type { ItemStatus, MenuRequest } from '../types/menu.types';
import toast from 'react-hot-toast';

// Query keys — dùng factory pattern để cache invalidation chính xác
export const menuKeys = {
  all:         ['menu']                        as const,
  list:        (params?: object) => ['menu', 'list', params] as const,
  detail:      (id: number) => ['menu', 'detail', id]        as const,
  bestSellers: ['menu', 'best-sellers']        as const,
};

export function useMenuList(query?: string, status?: ItemStatus) {
  return useQuery({
    queryKey: menuKeys.list({ query, status }),
    queryFn: () => menuApi.getAll({ query, status }),
    staleTime: 60_000, // 1 phút — menu không thay đổi liên tục
  });
}

export function useMenuDetail(id: number) {
  return useQuery({
    queryKey: menuKeys.detail(id),
    queryFn: () => menuApi.getById(id),
    enabled: !!id,
  });
}

export function useBestSellers() {
  return useQuery({
    queryKey: menuKeys.bestSellers,
    queryFn: menuApi.getBestSellers,
    staleTime: 5 * 60_000,
  });
}

export function useCreateMenuItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: MenuRequest) => menuApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: menuKeys.all });
      toast.success('Đã thêm món mới');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useUpdateMenuItem(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: MenuRequest) => menuApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: menuKeys.all });
      toast.success('Đã cập nhật món');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useSetMenuStatus(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (status: ItemStatus) => menuApi.setStatus(id, { status }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: menuKeys.all });
    },
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 5. Order API

### Endpoints

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| `POST` | `/api/orders` | Tạo order (qua OrderingFacade) | SERVER, MANAGER |
| `GET` | `/api/orders/{id}` | Chi tiết order | SERVER, MANAGER |
| `GET` | `/api/orders?tableId={id}` | Orders theo bàn | SERVER, MANAGER |
| `PATCH` | `/api/orders/{orderId}/items/{itemId}/note` | Thêm ghi chú | SERVER, MANAGER |
| `DELETE` | `/api/orders/{id}` | Hủy order | SERVER, MANAGER |

> **Header bắt buộc:** `X-User-Id: {userId}` khi tạo order.

### Types (`src/types/order.types.ts`)

```typescript
export type OrderStatus = 'DRAFT' | 'PLACED' | 'PAID' | 'CANCELLED';
export type OrderType   = 'DINE_IN' | 'TAKEAWAY' | 'DELIVERY';

export interface ItemRequest {
  menuItemId: number;
  quantity: number;
  specialNotes?: string | null;
  options?: Record<string, string>;
}

export interface OrderRequest {
  tableId: number | null;
  type: OrderType;
  items: ItemRequest[];
}

export interface OrderItemResponse {
  id: number;
  menuItemId: number;
  quantity: number;
  priceAtPurchase: number;
  lineTotal: number;
  specialNotes: string | null;
  options: Record<string, string> | null;
  isAllergyAlert: boolean;
}

export interface OrderResponse {
  id: number;
  orderCode: string;
  tableId: number | null;
  type: OrderType;
  status: OrderStatus;
  subTotal: number;
  createdAt: string;
  items: OrderItemResponse[];
}

export interface NoteRequest {
  note: string;
}
```

### API functions (`src/api/order.api.ts`)

```typescript
import { apiClient } from './client';
import type { OrderRequest, OrderResponse, NoteRequest } from '../types/order.types';

export const orderApi = {
  create: (data: OrderRequest, userId: number) =>
    apiClient.post<OrderResponse>('/api/orders', data, {
      headers: { 'X-User-Id': userId },
    }).then(r => r.data),

  getById: (id: number) =>
    apiClient.get<OrderResponse>(`/api/orders/${id}`).then(r => r.data),

  getByTable: (tableId: number) =>
    apiClient.get<OrderResponse[]>('/api/orders', { params: { tableId } })
      .then(r => r.data),

  addNote: (orderId: number, itemId: number, data: NoteRequest) =>
    apiClient.patch<OrderResponse>(
      `/api/orders/${orderId}/items/${itemId}/note`, data
    ).then(r => r.data),

  cancel: (id: number) =>
    apiClient.delete<OrderResponse>(`/api/orders/${id}`).then(r => r.data),
};
```

### Cart Store — quản lý draft order trên client

```typescript
// src/stores/cart.store.ts
import { create } from 'zustand';
import type { ItemRequest, OrderType } from '../types/order.types';

interface CartItem extends ItemRequest {
  name: string;       // hiển thị UI
  unitPrice: number;  // hiển thị UI
}

interface CartState {
  tableId: number | null;
  orderType: OrderType;
  items: CartItem[];
  setTable: (id: number | null) => void;
  setOrderType: (type: OrderType) => void;
  addItem: (item: CartItem) => void;
  removeItem: (menuItemId: number) => void;
  updateQuantity: (menuItemId: number, qty: number) => void;
  updateNote: (menuItemId: number, note: string) => void;
  clear: () => void;
  subTotal: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  tableId: null,
  orderType: 'DINE_IN',
  items: [],

  setTable: (tableId) => set({ tableId }),
  setOrderType: (orderType) => set({ orderType }),

  addItem: (item) => set(state => {
    const exists = state.items.find(i => i.menuItemId === item.menuItemId);
    if (exists) {
      return {
        items: state.items.map(i =>
          i.menuItemId === item.menuItemId
            ? { ...i, quantity: i.quantity + item.quantity }
            : i
        ),
      };
    }
    return { items: [...state.items, item] };
  }),

  removeItem: (menuItemId) => set(state => ({
    items: state.items.filter(i => i.menuItemId !== menuItemId),
  })),

  updateQuantity: (menuItemId, qty) => set(state => ({
    items: state.items.map(i =>
      i.menuItemId === menuItemId ? { ...i, quantity: qty } : i
    ).filter(i => i.quantity > 0),
  })),

  updateNote: (menuItemId, note) => set(state => ({
    items: state.items.map(i =>
      i.menuItemId === menuItemId ? { ...i, specialNotes: note } : i
    ),
  })),

  clear: () => set({ items: [], tableId: null }),

  subTotal: () => get().items.reduce(
    (sum, i) => sum + i.unitPrice * i.quantity, 0
  ),
}));
```

### Hooks (`src/hooks/useOrder.ts`)

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '../api/order.api';
import { useCartStore } from '../stores/cart.store';
import { useAuthStore } from '../stores/auth.store';
import type { OrderRequest } from '../types/order.types';
import toast from 'react-hot-toast';

export const orderKeys = {
  all:      ['orders']                          as const,
  byTable:  (tableId: number) => ['orders', 'table', tableId] as const,
  detail:   (id: number) => ['orders', 'detail', id]          as const,
};

export function useOrdersByTable(tableId: number) {
  return useQuery({
    queryKey: orderKeys.byTable(tableId),
    queryFn: () => orderApi.getByTable(tableId),
    enabled: !!tableId,
    refetchInterval: 15_000, // auto-refresh mỗi 15 giây
  });
}

export function useOrderDetail(id: number) {
  return useQuery({
    queryKey: orderKeys.detail(id),
    queryFn: () => orderApi.getById(id),
    enabled: !!id,
  });
}

export function usePlaceOrder() {
  const qc = useQueryClient();
  const cart = useCartStore();
  const userId = useAuthStore(s => s.userId) ?? 0;

  return useMutation({
    mutationFn: (data: OrderRequest) => orderApi.create(data, userId),
    onSuccess: (order) => {
      // Xóa cart sau khi đặt thành công
      cart.clear();
      qc.invalidateQueries({ queryKey: orderKeys.all });
      toast.success(`Order ${order.orderCode} đã được gửi tới bếp!`);
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useCancelOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: orderApi.cancel,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: orderKeys.all });
      toast.success('Order đã được hủy');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 6. Kitchen API (+ Station)

### Endpoints

| Method | Path | Query params | Role |
|--------|------|-------------|------|
| `GET` | `/api/kitchen/queue` | `stations`, `status`, `nearDeadline`, `sortBy` | CHEF, MANAGER |
| `GET` | `/api/kitchen/stations` | `type` | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/start` | `stationId` (required) | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/done` | — | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/pause` | — | CHEF, MANAGER |
| `PATCH` | `/api/kitchen/tickets/{id}/undo` | — | CHEF, MANAGER |
| `GET` | `/api/kitchen/sla` | `from`, `to` | MANAGER |

> **Lưu ý quan trọng:** `PATCH /start` **bắt buộc** phải có `?stationId={id}`. Thiếu → HTTP 400.

### Types (`src/types/kitchen.types.ts`)

```typescript
export type TicketStatus = 'QUEUED' | 'COOKING' | 'READY' | 'PAUSED' | 'DELIVERED';
export type StationStatus = 'AVAILABLE' | 'IN_USE' | 'OFFLINE';

// Thêm Interface cho MenuItem
export interface MenuItemResponse {
  id: number;
  name: string;
  basePrice: number;
  description: string;
  allergens: string;
  status: string;
}

// Thêm Interface cho OrderItem
export interface OrderItemResponse {
  id: number;
  menuItemId: number;
  quantity: number;
  specialNotes: string;
  options: Record<string, string>; // Map<String, String> trong Java
}

export interface StationResponse {
  id: number;
  name: string;
  type: string;
  status: StationStatus;
}

export interface KitchenTicketResponse {
  id: number;
  orderItem: OrderItemResponse; 
  menuItem: MenuItemResponse;   
  quantity: number;
  status: TicketStatus;
  startedAt: string | null;
  finishedAt: string | null;
  deadlineTime: string;
  nearDeadline: boolean;
  assignedStation: StationResponse | null;
}
```

### API functions (`src/api/kitchen.api.ts`)

```typescript
import { apiClient } from './client';
import type {
  KitchenTicketResponse,
  KitchenTicketFilter,
  StationResponse,
  SLAData,
} from '../types/kitchen.types';

export const kitchenApi = {
  getQueue: (filter?: KitchenTicketFilter) =>
    apiClient.get<KitchenTicketResponse[]>('/api/kitchen/queue', {
      params: filter,
    }).then(r => r.data),

  getStations: (type?: string) =>
    apiClient.get<StationResponse[]>('/api/kitchen/stations', {
      params: type ? { type } : undefined,
    }).then(r => r.data),

  startCooking: (ticketId: number, stationId: number) =>
    apiClient.patch<KitchenTicketResponse>(
      `/api/kitchen/tickets/${ticketId}/start`,
      null,
      { params: { stationId } }
    ).then(r => r.data),

  markDone: (ticketId: number) =>
    apiClient.patch<KitchenTicketResponse>(
      `/api/kitchen/tickets/${ticketId}/done`
    ).then(r => r.data),

  pause: (ticketId: number) =>
    apiClient.patch<KitchenTicketResponse>(
      `/api/kitchen/tickets/${ticketId}/pause`
    ).then(r => r.data),

  undo: (ticketId: number) =>
    apiClient.patch<KitchenTicketResponse>(
      `/api/kitchen/tickets/${ticketId}/undo`
    ).then(r => r.data),

  getSLA: (from: string, to: string) =>
    apiClient.get<SLAData[]>('/api/kitchen/sla', { params: { from, to } })
      .then(r => r.data),
};
```

### Hooks (`src/hooks/useKitchen.ts`)

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { kitchenApi } from '../api/kitchen.api';
import type { KitchenTicketFilter } from '../types/kitchen.types';
import toast from 'react-hot-toast';

export const kitchenKeys = {
  queue:    (filter?: KitchenTicketFilter) => ['kitchen', 'queue', filter] as const,
  stations: (type?: string) => ['kitchen', 'stations', type] as const,
};

// Auto-refresh queue mỗi 5 giây — gần realtime
export function useKitchenQueue(filter?: KitchenTicketFilter) {
  return useQuery({
    queryKey: kitchenKeys.queue(filter),
    queryFn: () => kitchenApi.getQueue(filter),
    refetchInterval: 5_000,
    refetchIntervalInBackground: true,
  });
}

export function useAvailableStations(type?: string) {
  return useQuery({
    queryKey: kitchenKeys.stations(type),
    queryFn: () => kitchenApi.getStations(type),
    staleTime: 3_000, // stale rất nhanh — station status thay đổi liên tục
  });
}

export function useStartCooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ ticketId, stationId }: { ticketId: number; stationId: number }) =>
      kitchenApi.startCooking(ticketId, stationId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['kitchen'] });
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useMarkDone() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: kitchenApi.markDone,
    onSuccess: (ticket) => {
      qc.invalidateQueries({ queryKey: ['kitchen'] });
      toast.success(`Ticket #${ticket.id} — READY!`);
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function usePauseTicket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: kitchenApi.pause,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['kitchen'] }),
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useUndoTicket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: kitchenApi.undo,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['kitchen'] }),
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 7. Payment API

### Endpoints

| Method | Path | Query params | Role |
|--------|------|-------------|------|
| `GET` | `/api/payments/orders/{orderId}/total` | — | CASHIER, MANAGER |
| `POST` | `/api/payments` | `orderId`, `amount`, `method`, `tip` | CASHIER, MANAGER |
| `POST` | `/api/payments/{id}/refund` | — | CASHIER, MANAGER |
| `GET` | `/api/payments/orders/{orderId}` | — | CASHIER, MANAGER |

### Types (`src/types/payment.types.ts`)

```typescript
export type PaymentMethod = 'CASH' | 'CREDIT_CARD' | 'ONLINE_BANKING' | 'E_WALLET';
export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

export interface OrderBillingDTO {
  orderId: number;
  orderCode: string;
  subTotal: number;
  discountAmount: number;
  taxAmount: number;
  total: number;
}

export interface PaymentTransaction {
  id: number;
  orderId: number;
  amount: number;
  tipAmount: number | null;
  taxAmount: number;
  discountAmount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  transactionTime: string;
  gatewayReferenceId: string | null;
}

export interface PaymentRequest {
  orderId: number;
  amount: number;
  method: PaymentMethod;
  tip?: number;
}
```

### API functions (`src/api/payment.api.ts`)

```typescript
import { apiClient } from './client';
import type { OrderBillingDTO, PaymentTransaction, PaymentRequest } from '../types/payment.types';

export const paymentApi = {
  getTotal: (orderId: number) =>
    apiClient.get<OrderBillingDTO>(
      `/api/payments/orders/${orderId}/total`
    ).then(r => r.data),

  pay: (data: PaymentRequest) =>
    apiClient.post<PaymentTransaction>('/api/payments', null, {
      params: {
        orderId: data.orderId,
        amount: data.amount,
        method: data.method,
        tip: data.tip,
      },
    }).then(r => r.data),

  refund: (transactionId: number) =>
    apiClient.post<PaymentTransaction>(
      `/api/payments/${transactionId}/refund`
    ).then(r => r.data),

  getByOrder: (orderId: number) =>
    apiClient.get<PaymentTransaction[]>(
      `/api/payments/orders/${orderId}`
    ).then(r => r.data),
};
```

### Hooks (`src/hooks/usePayment.ts`)

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../api/payment.api';
import type { PaymentRequest } from '../types/payment.types';
import toast from 'react-hot-toast';

export function useOrderBilling(orderId: number) {
  return useQuery({
    queryKey: ['payment', 'billing', orderId],
    queryFn: () => paymentApi.getTotal(orderId),
    enabled: !!orderId,
    staleTime: 30_000,
  });
}

export function useProcessPayment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: PaymentRequest) => paymentApi.pay(data),
    onSuccess: (tx) => {
      qc.invalidateQueries({ queryKey: ['orders'] });
      qc.invalidateQueries({ queryKey: ['seating'] });
      toast.success(`Thanh toán thành công! Ref: ${tx.gatewayReferenceId}`);
    },
    onError: (err: Error) => toast.error(err.message),
  });
}

export function useRefund() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: paymentApi.refund,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payment'] });
      toast.success('Hoàn tiền thành công');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 8. Promotion API

### Endpoints

| Method | Path | Role |
|--------|------|------|
| `GET` | `/api/promotions?status=ACTIVE` | MANAGER |
| `POST` | `/api/promotions` | MANAGER |
| `PUT` | `/api/promotions/{id}` | MANAGER |
| `DELETE` | `/api/promotions/{id}` | MANAGER |
| `PATCH` | `/api/promotions/{id}/status` | MANAGER |
| `POST` | `/api/promotions/simulate` | MANAGER |

### Types (`src/types/promotion.types.ts`)

```typescript
export type PromoType   = 'BY_PERCENT' | 'BY_AMOUNT' | 'COMBO' | 'BUY_X_GET_Y';
export type PromoStatus = 'ACTIVE' | 'INACTIVE' | 'EXPIRED';

export interface PromoRequest {
  name: string;
  promoType: PromoType;
  condition?: string;
  menuItemIds: number[];
  startDate?: string;    // ISO-8601: "2026-04-01T00:00:00"
  dueDate?: string;
  discountValue: number;
}

export interface PromoResponse {
  id: number;
  name: string;
  promoType: PromoType;
  condition: string | null;
  menuItemIds: number[];
  startDate: string | null;
  dueDate: string | null;
  discountValue: number;
  status: PromoStatus;
}

export interface SimulateRequest {
  menuItemQuantities: Record<number, number>;  // { menuItemId: quantity }
  subTotal: number;
}

export interface SimulationResult {
  originalSubTotal: number;
  discountAmount: number;
  finalTotal: number;
  appliedPromo: string;
}
```

### API + Hooks (`src/api/promotion.api.ts`)

```typescript
import { apiClient } from './client';
import type { PromoRequest, PromoResponse, SimulateRequest, SimulationResult, PromoStatus } from '../types/promotion.types';

export const promotionApi = {
  getAll: (status?: PromoStatus) =>
    apiClient.get<PromoResponse[]>('/api/promotions', {
      params: status ? { status } : undefined,
    }).then(r => r.data),

  create: (data: PromoRequest) =>
    apiClient.post<PromoResponse>('/api/promotions', data).then(r => r.data),

  update: (id: number, data: PromoRequest) =>
    apiClient.put<PromoResponse>(`/api/promotions/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    apiClient.delete<void>(`/api/promotions/${id}`),

  setStatus: (id: number, status: PromoStatus) =>
    apiClient.patch<PromoResponse>(`/api/promotions/${id}/status`, { status })
      .then(r => r.data),

  simulate: (data: SimulateRequest) =>
    apiClient.post<SimulationResult>('/api/promotions/simulate', data)
      .then(r => r.data),
};
```

---

## 9. Inventory API

### Endpoints

| Method | Path | Role |
|--------|------|------|
| `POST` | `/api/inventory/usage` | SERVER, CHEF, MANAGER |
| `GET` | `/api/inventory/alerts` | SERVER, CHEF, MANAGER |

### Types (`src/types/inventory.types.ts`)

```typescript
export type UsageReason = 'PREP' | 'SPOILAGE' | 'ADJUSTMENT' | 'AUTO_DEDUCT' | 'RESTOCK';

export interface UsageRequest {
  ingredientId: number;
  amount: number;
  reason: UsageReason;
}

export interface LowStockAlert {
  ingredientId: number;
  name: string;
  currentStock: number;
  minThreshold: number;
  unit: string;
}

export interface IngredientResponse {
  id: number;
  name: string;
  currentStock: number;
  minThreshold: number;
  unit: string;
  lastRestockDate: string | null;
}
```

### API + Hooks (`src/api/inventory.api.ts`)

```typescript
import { apiClient } from './client';
import type { UsageRequest, LowStockAlert } from '../types/inventory.types';

export const inventoryApi = {
  recordUsage: (requests: UsageRequest[], userId: number) =>
    apiClient.post<void>('/api/inventory/usage', requests, {
      headers: { 'X-User-Id': userId },
    }),

  getAll: () => 
    apiClient.get<IngredientResponse[]>('/api/inventory')
      .then(r => r.data),

  getAlerts: () =>
    apiClient.get<LowStockAlert[]>('/api/inventory/alerts').then(r => r.data),
};

// Hook
export function useLowStockAlerts() {
  return useQuery({
    queryKey: ['inventory', 'alerts'],
    queryFn: inventoryApi.getAlerts,
    refetchInterval: 60_000, // check mỗi 1 phút
  });
}
```

---

## 10. Seating API

### Endpoints

| Method | Path | Role |
|--------|------|------|
| `GET` | `/api/seating/tables` | SERVER, MANAGER |
| `POST` | `/api/seating/tables/{id}/seat-walkin?partySize={n}` | SERVER, MANAGER |
| `POST` | `/api/seating/reservations` | SERVER, MANAGER |
| `POST` | `/api/seating/reservations/{id}/seat` | SERVER, MANAGER |
| `POST` | `/api/seating/reservations/{id}/no-show` | SERVER, MANAGER |
| `GET` | `/api/seating/waitlist` | SERVER, MANAGER |
| `POST` | `/api/seating/waitlist` | SERVER, MANAGER |
| `POST` | `/api/seating/tables/move?from={}&to={}` | SERVER, MANAGER |
| `POST` | `/api/seating/tables/merge` | SERVER, MANAGER |

### Types (`src/types/seating.types.ts`)

```typescript
export type TableStatus =
  | 'AVAILABLE' | 'SEATED' | 'ORDERING'
  | 'SERVING' | 'CHECK_REQUESTED' | 'DIRTY' | 'LOCKED';

export type ReservationStatus = 'BOOKED' | 'SEATED' | 'NO_SHOW' | 'CANCELLED';

export interface TableResponse {
  id: number;
  tableCode: string;
  capacity: number;
  status: TableStatus;
}

export interface ReservationRequest {
  tableId?: number | null;
  customerName: string;
  customerPhone?: string;
  partySize: number;
  reservedTime: string;  // ISO-8601
}

export interface ReservationResponse {
  id: number;
  tableId: number | null;
  customerName: string;
  customerPhone: string | null;
  partySize: number;
  reservedTime: string;
  status: ReservationStatus;
}

export interface WaitlistRequest {
  customerName: string;
  partySize: number;
}

export interface WaitlistResponse {
  id: number;
  customerName: string;
  partySize: number;
  joinedAt: string;
  isNotified: boolean;
}
```

### API functions (`src/api/seating.api.ts`)

```typescript
import { apiClient } from './client';
import type {
  TableResponse, ReservationRequest, ReservationResponse,
  WaitlistRequest, WaitlistResponse,
} from '../types/seating.types';

export const seatingApi = {
  getTables: () =>
    apiClient.get<TableResponse[]>('/api/seating/tables').then(r => r.data),

  seatWalkIn: (tableId: number, partySize: number) =>
    apiClient.post<TableResponse>(
      `/api/seating/tables/${tableId}/seat-walkin`,
      null, { params: { partySize } }
    ).then(r => r.data),

  createReservation: (data: ReservationRequest) =>
    apiClient.post<ReservationResponse>('/api/seating/reservations', data)
      .then(r => r.data),

  seatReservation: (reservationId: number) =>
    apiClient.post<ReservationResponse>(
      `/api/seating/reservations/${reservationId}/seat`
    ).then(r => r.data),

  markNoShow: (reservationId: number) =>
    apiClient.post<ReservationResponse>(
      `/api/seating/reservations/${reservationId}/no-show`
    ).then(r => r.data),

  getWaitlist: () =>
    apiClient.get<WaitlistResponse[]>('/api/seating/waitlist').then(r => r.data),

  joinWaitlist: (data: WaitlistRequest) =>
    apiClient.post<WaitlistResponse>('/api/seating/waitlist', data)
      .then(r => r.data),

  moveTable: (from: number, to: number) =>
    apiClient.post<void>('/api/seating/tables/move', null, {
      params: { from, to },
    }),

  mergeTables: (tableIds: number[]) =>
    apiClient.post<void>('/api/seating/tables/merge', tableIds),
};
```

### Hooks (`src/hooks/useSeating.ts`)

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { seatingApi } from '../api/seating.api';
import toast from 'react-hot-toast';

export const seatingKeys = {
  tables:   ['seating', 'tables']   as const,
  waitlist: ['seating', 'waitlist'] as const,
};

export function useTables() {
  return useQuery({
    queryKey: seatingKeys.tables,
    queryFn: seatingApi.getTables,
    refetchInterval: 10_000,  // sơ đồ bàn tự refresh 10 giây
  });
}

export function useWaitlist() {
  return useQuery({
    queryKey: seatingKeys.waitlist,
    queryFn: seatingApi.getWaitlist,
    refetchInterval: 30_000,
  });
}

export function useSeatWalkIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ tableId, partySize }: { tableId: number; partySize: number }) =>
      seatingApi.seatWalkIn(tableId, partySize),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: seatingKeys.tables });
      toast.success('Khách đã được xếp bàn');
    },
    onError: (err: Error) => toast.error(err.message),
  });
}
```

---

## 11. Report API

### Endpoints

| Method | Path | Role |
|--------|------|------|
| `POST` | `/api/reports/dashboard` | MANAGER |
| `POST` | `/api/reports/export` | MANAGER |

### Types (`src/types/report.types.ts`)

```typescript
export type ReportType = 'SALE' | 'MENU' | 'SLA' | 'TABLE';

export interface ReportRequest {
  type: ReportType;
  startDate?: string;   // ISO-8601
  dueDate?: string;
}

export interface ReportData {
  summary: string;
  dataPoints: Record<string, unknown>;
}
```

### API + Hooks

```typescript
// src/api/report.api.ts
import { apiClient } from './client';
import type { ReportRequest, ReportData } from '../types/report.types';

export const reportApi = {
  dashboard: (data: ReportRequest) =>
    apiClient.post<ReportData>('/api/reports/dashboard', data).then(r => r.data),

  export: (data: ReportRequest) =>
    apiClient.post<string>('/api/reports/export', data).then(r => r.data),
};

// Hook
export function useReport(data: ReportRequest, enabled = true) {
  return useQuery({
    queryKey: ['reports', data.type, data.startDate, data.dueDate],
    queryFn: () => reportApi.dashboard(data),
    enabled,
    staleTime: 5 * 60_000,
  });
}
```

---

## 12. Error Handling toàn cục

### Pattern chuẩn trong component

```typescript
// Component nhận error từ hook → hiển thị toast hoặc inline
const { data, isLoading, error } = useMenuList();

if (isLoading) return <Spinner />;
if (error)     return <ErrorBanner message={error.message} />;
```

### Phân loại lỗi theo HTTP status

```typescript
// src/utils/error.utils.ts
import { AppError } from '../api/client';

export function getErrorMessage(error: unknown): string {
  if (error instanceof AppError) {
    switch (error.statusCode) {
      case 400: return `Dữ liệu không hợp lệ: ${error.message}`;
      case 401: return 'Phiên đăng nhập hết hạn, vui lòng đăng nhập lại';
      case 403: return 'Bạn không có quyền thực hiện thao tác này';
      case 404: return 'Không tìm thấy dữ liệu';
      case 409: return `Xung đột: ${error.message}`;
      case 422: return `Không thể xử lý: ${error.message}`;
      default:  return `Lỗi hệ thống (${error.statusCode}): ${error.message}`;
    }
  }
  return 'Đã xảy ra lỗi không xác định';
}
```

### RoleGuard component

```typescript
// src/components/layout/RoleGuard.tsx
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';

interface RoleGuardProps {
  allowedRoles: Array<'MANAGER' | 'SERVER' | 'CHEF' | 'CASHIER'>;
  children: React.ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { isAuthenticated, hasRole } = useAuthStore();

  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  if (!hasRole(...allowedRoles)) return <Navigate to="/unauthorized" replace />;

  return <>{children}</>;
}

// Dùng trong router:
// <RoleGuard allowedRoles={['MANAGER']}>
//   <ReportPage />
// </RoleGuard>
```

---

## 13. React Query patterns

### Query Client config (`src/main.tsx`)

```typescript
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,                // Thử lại 1 lần khi fail
      staleTime: 30_000,       // 30 giây trước khi refetch
      refetchOnWindowFocus: true,
    },
    mutations: {
      retry: 0,                // Không retry mutation (idempotency)
    },
  },
});
```

### Optimistic update (ví dụ: thay đổi status ticket)

```typescript
export function useStartCooking() {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ ticketId, stationId }: { ticketId: number; stationId: number }) =>
      kitchenApi.startCooking(ticketId, stationId),

    // Cập nhật UI ngay lập tức trước khi server confirm
    onMutate: async ({ ticketId }) => {
      await qc.cancelQueries({ queryKey: ['kitchen', 'queue'] });
      const prev = qc.getQueryData(['kitchen', 'queue']);

      qc.setQueryData(['kitchen', 'queue'], (old: any[]) =>
        old?.map(t =>
          t.id === ticketId ? { ...t, status: 'COOKING' } : t
        )
      );
      return { prev };
    },

    // Rollback nếu server fail
    onError: (_err, _vars, context) => {
      qc.setQueryData(['kitchen', 'queue'], context?.prev);
    },

    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['kitchen'] });
    },
  });
}
```

---

## 14. Hướng dẫn tổ chức Zustand stores

### Nguyên tắc

| Store | Chứa gì | Không chứa |
|-------|---------|------------|
| `auth.store` | token, username, role | Data từ API |
| `cart.store` | Items đang chọn, tableId | Order đã submitted |
| `kitchen.store` | Filter preferences, selected station | Ticket list (dùng React Query) |

### `kitchen.store.ts` — lưu UI state của màn hình bếp

```typescript
import { create } from 'zustand';

interface KitchenUIState {
  selectedFilter: 'ALL' | 'QUEUED' | 'COOKING' | 'READY';
  selectedStation: number | null;    // stationId đang chọn để start ticket
  setFilter: (f: KitchenUIState['selectedFilter']) => void;
  setSelectedStation: (id: number | null) => void;
}

export const useKitchenStore = create<KitchenUIState>((set) => ({
  selectedFilter: 'ALL',
  selectedStation: null,
  setFilter: (selectedFilter) => set({ selectedFilter }),
  setSelectedStation: (selectedStation) => set({ selectedStation }),
}));
```

---

## Quick Start

```bash
# Tạo project
npm create vite@latest restaurant-frontend -- --template react-ts
cd restaurant-frontend

# Cài dependencies
npm install \
  axios \
  @tanstack/react-query \
  zustand \
  react-router-dom \
  react-hot-toast \
  date-fns

npm install -D tailwindcss @tailwindcss/vite

# Tạo .env.local
echo "VITE_API_URL=http://localhost:8080" > .env.local
```

**Thứ tự implement:**

```
1. api/client.ts          → Axios + interceptors
2. types/*.types.ts       → Tất cả TypeScript types
3. stores/auth.store.ts   → Auth state
4. api/auth.api.ts        → Login function
5. hooks/useAuth.ts       → Login hook
6. router/index.tsx       → Routes + RoleGuard
7. api/*.api.ts           → Từng domain
8. hooks/use*.ts          → Từng domain
9. stores/cart.store.ts   → Cart cho SERVER
10. pages/**              → UI pages
```

---

> **Backend base URL:** `http://localhost:8080`
> **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
> **Tài khoản mock:** `admin / 123456` · `server_1 / 123456` · `chef_1 / 123456` · `cashier_1 / 123456`
