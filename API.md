{
  "openapi": "3.0.1",
  "info": {
    "title": "Restaurant Management System API",
    "description": "Hệ thống quản lý nhà hàng trung tâm.\n\n**Xác thực:**\n1. Gọi `POST /api/auth/login` để lấy JWT token\n2. Click nút **Authorize** 🔒 ở góc trên phải\n3. Nhập `Bearer \u003Ctoken\u003E` vào ô `bearerAuth`\n\n**Roles:**\n- `MANAGER` — Toàn quyền\n- `SERVER` — Orders, Menu (read), Seating, Inventory\n- `CHEF` — Kitchen, Menu (read)\n- `CASHIER` — Payments, Orders (read)\n",
    "contact": {
      "name": "SoftArch Team",
      "email": "dev@softarch.restaurant"
    },
    "license": {
      "name": "Proprietary",
      "url": "https://softarch.restaurant"
    },
    "version": "1.0.0"
  },
  "servers": [
    {
      "url": "http://localhost:8080",
      "description": "Local development server"
    },
    {
      "url": "https://api.restaurant.example.com",
      "description": "Production server"
    }
  ],
  "security": [
    {
      "bearerAuth": []
    }
  ],
  "tags": [
    {
      "name": "Auth",
      "description": "Xác thực — đăng nhập, lấy JWT token"
    }
  ],
  "paths": {
    "/api/promotions/{id}": {
      "put": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "updateItem",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/PromoRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponsePromoResponse"
                }
              }
            }
          }
        }
      },
      "delete": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "delete",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseVoid"
                }
              }
            }
          }
        }
      }
    },
    "/api/menu/{id}": {
      "get": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "getById_1",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseMenuItemResponse"
                }
              }
            }
          }
        }
      },
      "put": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "update",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/MenuRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseMenuItemResponse"
                }
              }
            }
          }
        }
      },
      "delete": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "delete_1",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseVoid"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/waitlist": {
      "get": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "waitlist",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListWaitlistResponse"
                }
              }
            }
          }
        }
      },
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "joinWaitlist",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/WaitlistRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseWaitlistResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/tables/{id}/seat-walkin": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "seatWalkIn",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          },
          {
            "name": "partySize",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseTableResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/tables/move": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "moveTable",
        "parameters": [
          {
            "name": "from",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          },
          {
            "name": "to",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseVoid"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/tables/merge": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "mergeTables",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "type": "array",
                "items": {
                  "type": "integer",
                  "format": "int64"
                }
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseVoid"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/reservations": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "createReservation",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/ReservationRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseReservationResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/reservations/{id}/seat": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "seatReservation",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseReservationResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/reservations/{id}/no-show": {
      "post": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "noShow",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseReservationResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/promotions": {
      "get": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "list",
        "parameters": [
          {
            "name": "status",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string",
              "default": "ACTIVE",
              "enum": [
                "ACTIVE",
                "INACTIVE",
                "EXPIRED"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListPromoResponse"
                }
              }
            }
          }
        }
      },
      "post": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "createPromo",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/PromoRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponsePromoResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/promotions/simulate": {
      "post": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "simulate",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/SimulateRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseSimulationResult"
                }
              }
            }
          }
        }
      }
    },
    "/api/payments": {
      "post": {
        "tags": [
          "payment-controller"
        ],
        "operationId": "createPayment",
        "parameters": [
          {
            "name": "orderId",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          },
          {
            "name": "amount",
            "in": "query",
            "required": true,
            "schema": {
              "type": "number"
            }
          },
          {
            "name": "method",
            "in": "query",
            "required": true,
            "schema": {
              "type": "string",
              "enum": [
                "CASH",
                "CREDIT_CARD",
                "E_WALLET"
              ]
            }
          },
          {
            "name": "tip",
            "in": "query",
            "required": false,
            "schema": {
              "type": "number",
              "default": 0
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponsePaymentTransaction"
                }
              }
            }
          }
        }
      }
    },
    "/api/payments/{id}/refund": {
      "post": {
        "tags": [
          "payment-controller"
        ],
        "operationId": "refund",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponsePaymentTransaction"
                }
              }
            }
          }
        }
      }
    },
    "/api/orders": {
      "get": {
        "tags": [
          "order-controller"
        ],
        "operationId": "getByTable",
        "parameters": [
          {
            "name": "tableId",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListOrderResponse"
                }
              }
            }
          }
        }
      },
      "post": {
        "tags": [
          "order-controller"
        ],
        "operationId": "createOrder",
        "parameters": [
          {
            "name": "X-User-Id",
            "in": "header",
            "required": false,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/OrderRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseOrderResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/menu": {
      "get": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "viewMenu",
        "parameters": [
          {
            "name": "query",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "status",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string",
              "enum": [
                "ACTIVE",
                "INACTIVE",
                "OUT_OF_STOCK"
              ]
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListMenuItemResponse"
                }
              }
            }
          }
        }
      },
      "post": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "create",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/MenuRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseMenuItemResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/inventory/usage": {
      "post": {
        "tags": [
          "inventory-controller"
        ],
        "operationId": "recordUsage",
        "parameters": [
          {
            "name": "X-User-Id",
            "in": "header",
            "required": false,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "type": "array",
                "items": {
                  "$ref": "#/components/schemas/UsageRequest"
                }
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseVoid"
                }
              }
            }
          }
        }
      }
    },
    "/api/auth/validate": {
      "post": {
        "tags": [
          "Auth"
        ],
        "operationId": "validate",
        "parameters": [
          {
            "name": "Authorization",
            "in": "header",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseBoolean"
                }
              }
            }
          }
        }
      }
    },
    "/api/auth/login": {
      "post": {
        "tags": [
          "Auth"
        ],
        "operationId": "login",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/LoginRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseLoginResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/promotions/{id}/status": {
      "patch": {
        "tags": [
          "promo-controller"
        ],
        "operationId": "setStatus",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/StatusRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponsePromoResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/orders/{orderId}/items/{itemId}/note": {
      "patch": {
        "tags": [
          "order-controller"
        ],
        "operationId": "addNote",
        "parameters": [
          {
            "name": "orderId",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          },
          {
            "name": "itemId",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/NoteRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseOrderResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/menu/{id}/status": {
      "patch": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "setStatus_1",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/StatusRequest"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseMenuItemResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/tickets/{id}/undo": {
      "patch": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "undo",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseKitchenTicketResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/tickets/{id}/start": {
      "patch": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "start",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          },
          {
            "name": "stationId",
            "in": "query",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseKitchenTicketResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/tickets/{id}/pause": {
      "patch": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "pause",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseKitchenTicketResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/tickets/{id}/done": {
      "patch": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "done",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseKitchenTicketResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/seating/tables": {
      "get": {
        "tags": [
          "seating-controller"
        ],
        "operationId": "tables",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListTableResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/payments/orders/{orderId}": {
      "get": {
        "tags": [
          "payment-controller"
        ],
        "operationId": "listByOrder",
        "parameters": [
          {
            "name": "orderId",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListPaymentTransaction"
                }
              }
            }
          }
        }
      }
    },
    "/api/payments/orders/{orderId}/total": {
      "get": {
        "tags": [
          "payment-controller"
        ],
        "operationId": "getTotal",
        "parameters": [
          {
            "name": "orderId",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseOrderBillingDTO"
                }
              }
            }
          }
        }
      }
    },
    "/api/orders/{id}": {
      "get": {
        "tags": [
          "order-controller"
        ],
        "operationId": "getById",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseOrderResponse"
                }
              }
            }
          }
        }
      },
      "delete": {
        "tags": [
          "order-controller"
        ],
        "operationId": "cancel",
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int64"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseOrderResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/menu/best-sellers": {
      "get": {
        "tags": [
          "menu-controller"
        ],
        "operationId": "bestSellers",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListMenuItemResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/stations": {
      "get": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "getAvailableStations",
        "parameters": [
          {
            "name": "type",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListStationResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/sla": {
      "get": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "sla",
        "parameters": [
          {
            "name": "from",
            "in": "query",
            "required": true,
            "schema": {
              "type": "string",
              "format": "date-time"
            }
          },
          {
            "name": "to",
            "in": "query",
            "required": true,
            "schema": {
              "type": "string",
              "format": "date-time"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListSLAData"
                }
              }
            }
          }
        }
      }
    },
    "/api/kitchen/queue": {
      "get": {
        "tags": [
          "kitchen-controller"
        ],
        "operationId": "getQueue",
        "parameters": [
          {
            "name": "stations",
            "in": "query",
            "required": false,
            "schema": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          },
          {
            "name": "status",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "nearDeadline",
            "in": "query",
            "required": false,
            "schema": {
              "type": "boolean"
            }
          },
          {
            "name": "sortBy",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string",
              "default": "deadline"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListKitchenTicketResponse"
                }
              }
            }
          }
        }
      }
    },
    "/api/inventory/alerts": {
      "get": {
        "tags": [
          "inventory-controller"
        ],
        "operationId": "alerts",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "*/*": {
                "schema": {
                  "$ref": "#/components/schemas/ApiResponseListLowStockAlert"
                }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "PromoRequest": {
        "required": [
          "discountValue",
          "menuItemIds",
          "name",
          "promoType"
        ],
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "promoType": {
            "type": "string",
            "enum": [
              "BY_PERCENT",
              "BY_AMOUNT",
              "COMBO",
              "BUY_X_GET_Y"
            ]
          },
          "condition": {
            "type": "string"
          },
          "menuItemIds": {
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            }
          },
          "startDate": {
            "type": "string",
            "format": "date-time"
          },
          "dueDate": {
            "type": "string",
            "format": "date-time"
          },
          "discountValue": {
            "type": "number"
          }
        }
      },
      "ApiResponsePromoResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/PromoResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "PromoResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "name": {
            "type": "string"
          },
          "promoType": {
            "type": "string",
            "enum": [
              "BY_PERCENT",
              "BY_AMOUNT",
              "COMBO",
              "BUY_X_GET_Y"
            ]
          },
          "condition": {
            "type": "string"
          },
          "menuItemIds": {
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            }
          },
          "startDate": {
            "type": "string",
            "format": "date-time"
          },
          "dueDate": {
            "type": "string",
            "format": "date-time"
          },
          "discountValue": {
            "type": "number"
          },
          "status": {
            "type": "string",
            "enum": [
              "ACTIVE",
              "INACTIVE",
              "EXPIRED"
            ]
          }
        }
      },
      "MenuRequest": {
        "required": [
          "basePrice",
          "name"
        ],
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "basePrice": {
            "minimum": 0,
            "exclusiveMinimum": true,
            "type": "number"
          },
          "description": {
            "type": "string"
          },
          "allergens": {
            "type": "array",
            "items": {
              "type": "string"
            }
          }
        }
      },
      "ApiResponseMenuItemResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/MenuItemResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "MenuItemResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "name": {
            "type": "string"
          },
          "basePrice": {
            "type": "number"
          },
          "description": {
            "type": "string"
          },
          "allergens": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "status": {
            "type": "string",
            "enum": [
              "ACTIVE",
              "INACTIVE",
              "OUT_OF_STOCK"
            ]
          }
        }
      },
      "WaitlistRequest": {
        "required": [
          "customerName",
          "partySize"
        ],
        "type": "object",
        "properties": {
          "customerName": {
            "type": "string"
          },
          "partySize": {
            "minimum": 1,
            "type": "integer",
            "format": "int32"
          }
        }
      },
      "ApiResponseWaitlistResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/WaitlistResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "WaitlistResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "customerName": {
            "type": "string"
          },
          "partySize": {
            "type": "integer",
            "format": "int32"
          },
          "joinedAt": {
            "type": "string",
            "format": "date-time"
          },
          "isNotified": {
            "type": "boolean"
          }
        }
      },
      "ApiResponseTableResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/TableResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "TableResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "tableCode": {
            "type": "string"
          },
          "capacity": {
            "type": "integer",
            "format": "int32"
          },
          "status": {
            "type": "string"
          }
        }
      },
      "ApiResponseVoid": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "object"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ReservationRequest": {
        "required": [
          "customerName",
          "partySize",
          "reservedTime"
        ],
        "type": "object",
        "properties": {
          "tableId": {
            "type": "integer",
            "format": "int64"
          },
          "customerName": {
            "type": "string"
          },
          "customerPhone": {
            "type": "string"
          },
          "partySize": {
            "minimum": 1,
            "type": "integer",
            "format": "int32"
          },
          "reservedTime": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseReservationResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/ReservationResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ReservationResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "tableId": {
            "type": "integer",
            "format": "int64"
          },
          "customerName": {
            "type": "string"
          },
          "customerPhone": {
            "type": "string"
          },
          "partySize": {
            "type": "integer",
            "format": "int32"
          },
          "reservedTime": {
            "type": "string",
            "format": "date-time"
          },
          "status": {
            "type": "string"
          }
        }
      },
      "SimulateRequest": {
        "type": "object",
        "properties": {
          "menuItemQuantities": {
            "type": "object",
            "additionalProperties": {
              "type": "integer",
              "format": "int32"
            }
          },
          "subTotal": {
            "type": "number"
          }
        }
      },
      "ApiResponseSimulationResult": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/SimulationResult"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "SimulationResult": {
        "type": "object",
        "properties": {
          "originalSubTotal": {
            "type": "number"
          },
          "discountAmount": {
            "type": "number"
          },
          "finalTotal": {
            "type": "number"
          },
          "appliedPromo": {
            "type": "string"
          }
        }
      },
      "ApiResponsePaymentTransaction": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/PaymentTransaction"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "PaymentTransaction": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "orderId": {
            "type": "integer",
            "format": "int64"
          },
          "amount": {
            "type": "number"
          },
          "tipAmount": {
            "type": "number"
          },
          "taxAmount": {
            "type": "number"
          },
          "discountAmount": {
            "type": "number"
          },
          "method": {
            "type": "string",
            "enum": [
              "CASH",
              "CREDIT_CARD",
              "E_WALLET"
            ]
          },
          "status": {
            "type": "string",
            "enum": [
              "PENDING",
              "COMPLETED",
              "FAILED",
              "REFUNDED"
            ]
          },
          "transactionTime": {
            "type": "string",
            "format": "date-time"
          },
          "gatewayReferenceId": {
            "type": "string"
          }
        }
      },
      "ItemRequest": {
        "required": [
          "menuItemId",
          "quantity"
        ],
        "type": "object",
        "properties": {
          "menuItemId": {
            "type": "integer",
            "format": "int64"
          },
          "quantity": {
            "minimum": 1,
            "type": "integer",
            "format": "int32"
          },
          "specialNotes": {
            "type": "string"
          },
          "options": {
            "type": "object",
            "additionalProperties": {
              "type": "string"
            }
          }
        }
      },
      "OrderRequest": {
        "required": [
          "items",
          "type"
        ],
        "type": "object",
        "properties": {
          "tableId": {
            "type": "integer",
            "format": "int64"
          },
          "type": {
            "type": "string",
            "enum": [
              "DINE_IN",
              "TAKEAWAY",
              "DELIVERY"
            ]
          },
          "items": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/ItemRequest"
            }
          }
        }
      },
      "ApiResponseOrderResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/OrderResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "OrderItemResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "menuItemId": {
            "type": "integer",
            "format": "int64"
          },
          "quantity": {
            "type": "integer",
            "format": "int32"
          },
          "priceAtPurchase": {
            "type": "number"
          },
          "lineTotal": {
            "type": "number"
          },
          "specialNotes": {
            "type": "string"
          },
          "options": {
            "type": "object",
            "additionalProperties": {
              "type": "string"
            }
          },
          "isAllergyAlert": {
            "type": "boolean"
          }
        }
      },
      "OrderResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "orderCode": {
            "type": "string"
          },
          "tableId": {
            "type": "integer",
            "format": "int64"
          },
          "type": {
            "type": "string",
            "enum": [
              "DINE_IN",
              "TAKEAWAY",
              "DELIVERY"
            ]
          },
          "status": {
            "type": "string",
            "enum": [
              "DRAFT",
              "PLACED",
              "PAID",
              "CANCELLED"
            ]
          },
          "subTotal": {
            "type": "number"
          },
          "createdAt": {
            "type": "string",
            "format": "date-time"
          },
          "items": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/OrderItemResponse"
            }
          }
        }
      },
      "UsageRequest": {
        "required": [
          "amount",
          "ingredientId",
          "reason"
        ],
        "type": "object",
        "properties": {
          "ingredientId": {
            "type": "integer",
            "format": "int64"
          },
          "amount": {
            "minimum": 0.001,
            "exclusiveMinimum": false,
            "type": "number"
          },
          "reason": {
            "type": "string",
            "enum": [
              "PREP",
              "SPOILAGE",
              "ADJUSTMENT",
              "AUTO_DEDUCT",
              "RESTOCK"
            ]
          }
        }
      },
      "ApiResponseBoolean": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "boolean"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "LoginRequest": {
        "required": [
          "password",
          "username"
        ],
        "type": "object",
        "properties": {
          "username": {
            "type": "string"
          },
          "password": {
            "type": "string"
          }
        }
      },
      "ApiResponseLoginResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/LoginResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "LoginResponse": {
        "type": "object",
        "properties": {
          "token": {
            "type": "string"
          },
          "username": {
            "type": "string"
          },
          "role": {
            "type": "string"
          },
          "expiresInSeconds": {
            "type": "integer",
            "format": "int64"
          }
        }
      },
      "StatusRequest": {
        "required": [
          "status"
        ],
        "type": "object",
        "properties": {
          "status": {
            "type": "string",
            "enum": [
              "ACTIVE",
              "INACTIVE",
              "EXPIRED"
            ]
          }
        }
      },
      "NoteRequest": {
        "required": [
          "note"
        ],
        "type": "object",
        "properties": {
          "note": {
            "type": "string"
          }
        }
      },
      "ApiResponseKitchenTicketResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/KitchenTicketResponse"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "KitchenTicketResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "orderItemId": {
            "type": "integer",
            "format": "int64"
          },
          "menuItemId": {
            "type": "integer",
            "format": "int64"
          },
          "quantity": {
            "type": "integer",
            "format": "int32"
          },
          "status": {
            "type": "string"
          },
          "startedAt": {
            "type": "string",
            "format": "date-time"
          },
          "finishedAt": {
            "type": "string",
            "format": "date-time"
          },
          "deadlineTime": {
            "type": "string",
            "format": "date-time"
          },
          "nearDeadline": {
            "type": "boolean"
          },
          "assignedStation": {
            "$ref": "#/components/schemas/StationResponse"
          }
        }
      },
      "StationResponse": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "format": "int64"
          },
          "name": {
            "type": "string"
          },
          "type": {
            "type": "string"
          },
          "status": {
            "type": "string"
          }
        }
      },
      "ApiResponseListWaitlistResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/WaitlistResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListTableResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/TableResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListPromoResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/PromoResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListPaymentTransaction": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/PaymentTransaction"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseOrderBillingDTO": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "$ref": "#/components/schemas/OrderBillingDTO"
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "OrderBillingDTO": {
        "type": "object",
        "properties": {
          "orderId": {
            "type": "integer",
            "format": "int64"
          },
          "orderCode": {
            "type": "string"
          },
          "subTotal": {
            "type": "number"
          },
          "discountAmount": {
            "type": "number"
          },
          "taxAmount": {
            "type": "number"
          },
          "total": {
            "type": "number"
          }
        }
      },
      "ApiResponseListOrderResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/OrderResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListMenuItemResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/MenuItemResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListStationResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/StationResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListSLAData": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/SLAData"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "SLAData": {
        "type": "object",
        "properties": {
          "menuItemId": {
            "type": "integer",
            "format": "int64"
          },
          "menuItemName": {
            "type": "string"
          },
          "avgMinutesToComplete": {
            "type": "number",
            "format": "double"
          },
          "totalTickets": {
            "type": "integer",
            "format": "int64"
          },
          "overdueTickets": {
            "type": "integer",
            "format": "int64"
          }
        }
      },
      "ApiResponseListKitchenTicketResponse": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/KitchenTicketResponse"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "ApiResponseListLowStockAlert": {
        "type": "object",
        "properties": {
          "success": {
            "type": "boolean"
          },
          "data": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/LowStockAlert"
            }
          },
          "message": {
            "type": "string"
          },
          "timestamp": {
            "type": "string",
            "format": "date-time"
          }
        }
      },
      "LowStockAlert": {
        "type": "object",
        "properties": {
          "ingredientId": {
            "type": "integer",
            "format": "int64"
          },
          "name": {
            "type": "string"
          },
          "currentStock": {
            "type": "number"
          },
          "minThreshold": {
            "type": "number"
          },
          "unit": {
            "type": "string"
          }
        }
      }
    },
    "securitySchemes": {
      "bearerAuth": {
        "type": "http",
        "description": "Nhập JWT token lấy từ POST /api/auth/login. Format: Bearer \u003Ctoken\u003E",
        "scheme": "bearer",
        "bearerFormat": "JWT"
      }
    }
  }
}