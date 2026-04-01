package softarch.restaurant.domain.kitchen.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import softarch.restaurant.domain.kitchen.service.KitchenService;
import softarch.restaurant.domain.order.event.OrderPlacedEvent;
import softarch.restaurant.domain.order.entity.OrderItem;

import java.util.List;

/**
 * Matches diagram: OrderPlacedListener — triggers createTicket().
 * Listens for OrderPlacedEvent published by OrderServiceImpl and creates
 * one KitchenTicket per OrderItem.
 */
@Component
public class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final KitchenService kitchenService;

    public OrderPlacedListener(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("OrderPlacedEvent received for orderId={}. Creating kitchen tickets.", event.getOrderId());

        List<Long> orderItemIds = event.getItems().stream()
            .map(OrderItem::getId)
            .toList();

        kitchenService.createTicketsForOrder(event.getOrderId(), orderItemIds);
    }
} 