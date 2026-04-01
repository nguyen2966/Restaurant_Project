package softarch.restaurant.domain.seating.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import softarch.restaurant.domain.order.event.OrderPaidEvent;
import softarch.restaurant.domain.order.repository.OrderRepository;
import softarch.restaurant.domain.seating.service.SeatingService;

/**
 * Matches diagram: OrderPaymentListener — triggers clearTable().
 * Listens for OrderPaidEvent and marks the table as DIRTY so it can be cleaned
 * before the next seating.
 */
@Component
public class OrderPaymentListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentListener.class);

    private final SeatingService  seatingService;
    private final OrderRepository orderRepository;

    public OrderPaymentListener(SeatingService seatingService,
                                OrderRepository orderRepository) {
        this.seatingService  = seatingService;
        this.orderRepository = orderRepository;
    }

    @EventListener
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if (order.getTableId() != null) {
                log.info("Order {} paid — clearing tableId={}", event.getOrderId(), order.getTableId());
                seatingService.clearTable(order.getTableId());
            }
        });
    }
}