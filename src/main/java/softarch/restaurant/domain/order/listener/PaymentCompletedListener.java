package softarch.restaurant.domain.order.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.domain.payment.event.PaymentCompletedEvent;

/**
 * Listens for PaymentCompletedEvent and transitions the order to PAID.
 *
 * Runs synchronously (same transaction as payment) so that a payment success
 * always results in a consistent order status update.
 */
@Component
public class PaymentCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedListener.class);

    private final OrderService orderService;

    public PaymentCompletedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for orderId={}. Marking order as paid.", event.getOrderId());
        orderService.markAsPaid(event.getOrderId());
    }
}