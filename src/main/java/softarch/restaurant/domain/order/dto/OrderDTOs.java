package softarch.restaurant.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.entity.OrderItem;
import softarch.restaurant.domain.order.entity.OrderStatus;
import softarch.restaurant.domain.order.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class OrderDTOs {

    private OrderDTOs() {}

    // ── Inbound ───────────────────────────────────────────────────────────────

    public record ItemRequest(
        @NotNull(message = "menuItemId is required")
        Long menuItemId,

        @NotNull @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        String specialNotes,
        Map<String, String> options
    ) {}

    public record OrderRequest(
        Long tableId,           // null for TAKEAWAY / DELIVERY

        @NotNull(message = "type is required")
        OrderType type,

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<ItemRequest> items
    ) {}

    public record NoteRequest(
        @NotNull(message = "note is required")
        String note
    ) {}

    // ── Outbound ─────────────────────────────────────────────────────────────

    public record OrderItemResponse(
        Long            id,
        Long            menuItemId,
        Integer         quantity,
        BigDecimal      priceAtPurchase,
        BigDecimal      lineTotal,
        String          specialNotes,
        Map<String,String> options,
        boolean         isAllergyAlert
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                item.lineTotal(),
                item.getSpecialNotes(),
                item.getOptions(),
                item.isAllergyAlert()
            );
        }
    }

    public record OrderResponse(
        Long                    id,
        String                  orderCode,
        Long                    tableId,
        OrderType               type,
        OrderStatus             status,
        BigDecimal              subTotal,
        LocalDateTime           createdAt,
        List<OrderItemResponse> items
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getTableId(),
                order.getType(),
                order.getStatus(),
                order.getSubTotal(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
            );
        }
    }
}