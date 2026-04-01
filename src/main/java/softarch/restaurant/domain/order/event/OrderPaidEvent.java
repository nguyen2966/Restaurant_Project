package softarch.restaurant.domain.order.event;

import java.time.LocalDateTime;

/**
 * Published by OrderServiceImpl after markAsPaid() is called.
 * Can be consumed for analytics, receipts, loyalty points, etc.
 */
public class OrderPaidEvent {

    private final Long          orderId;
    private final LocalDateTime timestamp;

    public OrderPaidEvent(Long orderId) {
        this.orderId   = orderId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getOrderId()            { return orderId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}