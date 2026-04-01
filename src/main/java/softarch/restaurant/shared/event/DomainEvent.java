package softarch.restaurant.shared.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for all domain events in the system.
 * Carries a correlationId for distributed tracing and a timestamp.
 *
 * All events (OrderPlacedEvent, OrderPaidEvent, PaymentCompletedEvent,
 * KitchenItemDoneEvent) should extend this class.
 */
public abstract class DomainEvent {

    private final String        correlationId;
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.correlationId = UUID.randomUUID().toString();
        this.occurredAt    = LocalDateTime.now();
    }

    public String        getCorrelationId() { return correlationId; }
    public LocalDateTime getOccurredAt()    { return occurredAt; }
}
