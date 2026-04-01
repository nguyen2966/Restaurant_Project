package softarch.restaurant.domain.payment.event;

/**
 * Published by PaymentServiceImpl upon successful payment.
 * Defined here so Order domain can consume it without importing Payment domain internals.
 *
 * In a microservices migration, this becomes a message on a queue (Kafka/RabbitMQ).
 */
public class PaymentCompletedEvent {

    private final Long   orderId;
    private final double amountPaid;
    private final String method;

    public PaymentCompletedEvent(Long orderId, double amountPaid, String method) {
        this.orderId    = orderId;
        this.amountPaid = amountPaid;
        this.method     = method;
    }

    public Long   getOrderId()    { return orderId; }
    public double getAmountPaid() { return amountPaid; }
    public String getMethod()     { return method; }
} 
