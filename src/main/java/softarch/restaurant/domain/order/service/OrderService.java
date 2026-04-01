package softarch.restaurant.domain.order.service;

import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.entity.Order;

import java.util.List;

public interface OrderService {

    /**
     * Persists a fully-built Order (DRAFT → PLACED) and publishes OrderPlacedEvent.
     * Called exclusively by OrderingFacade after all validations pass.
     */
    OrderResponse placeOrder(Order order);

    /**
     * Loads a full Order entity by ID for use in facade orchestration.
     * Throws RestaurantException.notFound if absent.
     */
    Order getOrderEntity(Long orderId);

    /** Returns a response DTO for a single order. */
    OrderResponse getById(Long orderId);

    /** Returns all orders for the given table. */
    List<OrderResponse> getByTable(Long tableId);

    /** Adds a special note to a specific order item. */
    OrderResponse addNoteToItem(Long orderId, Long itemId, String note);

    /** Cancels an order. Throws if already paid. */
    OrderResponse cancelOrder(Long orderId);

    /**
     * Transitions order status to PAID.
     * Called by PaymentCompletedListener after a successful payment.
     */
    OrderResponse markAsPaid(Long orderId);

    /**
     * Returns true if the given menu item is in any non-terminal order.
     * Used by MenuService.isItemInActiveOrder().
     */
    boolean isItemInActiveOrder(Long menuItemId);
}