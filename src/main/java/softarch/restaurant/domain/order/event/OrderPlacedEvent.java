package softarch.restaurant.domain.order.event;

import softarch.restaurant.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Published by OrderServiceImpl after an order is confirmed and sent to the kitchen.
 * Consumed by the Kitchen domain to create KitchenTickets.
 */
public class OrderPlacedEvent {

    private final Long            orderId;
    private final List<OrderItem> items;
    private final LocalDateTime   timestamp;

    public OrderPlacedEvent(Long orderId, List<OrderItem> items) {
        this.orderId   = orderId;
        this.items     = List.copyOf(items);
        this.timestamp = LocalDateTime.now();
    }

    public Long getOrderId()          { return orderId; }
    public List<OrderItem> getItems() { return items; }
    public LocalDateTime getTimestamp() { return timestamp; }
}