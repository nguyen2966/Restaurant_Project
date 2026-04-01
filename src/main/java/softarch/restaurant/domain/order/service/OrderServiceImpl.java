package softarch.restaurant.domain.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.entity.OrderItem;
import softarch.restaurant.domain.order.event.OrderPaidEvent;
import softarch.restaurant.domain.order.event.OrderPlacedEvent;
import softarch.restaurant.domain.order.repository.OrderItemRepository;
import softarch.restaurant.domain.order.repository.OrderRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository      orderRepository;
    private final OrderItemRepository  orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository     = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher      = eventPublisher;
    }

    // ── Core placement (called by OrderingFacade) ─────────────────────────────

    @Override
    public OrderResponse placeOrder(Order order) {
        order.confirm();                          // DRAFT → PLACED (validates not empty)
        Order saved = orderRepository.save(order);

        log.info("Order placed: {} ({})", saved.getOrderCode(), saved.getId());

        // Publish event so Kitchen domain can create KitchenTickets
        eventPublisher.publishEvent(new OrderPlacedEvent(saved.getId(), saved.getItems()));

        return OrderResponse.from(saved);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Order getOrderEntity(Long orderId) {
        return findOrThrow(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        return OrderResponse.from(findOrThrow(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByTable(Long tableId) {
        return orderRepository.findByTableId(tableId)
            .stream().map(OrderResponse::from).toList();
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Override
    public OrderResponse addNoteToItem(Long orderId, Long itemId, String note) {
        Order order = findOrThrow(orderId);
        OrderItem item = order.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> RestaurantException.notFound("OrderItem", itemId));
        item.addNote(note);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    public OrderResponse cancelOrder(Long orderId) {
        Order order = findOrThrow(orderId);
        order.cancel();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    public OrderResponse markAsPaid(Long orderId) {
        Order order = findOrThrow(orderId);
        order.markAsPaid();
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPaidEvent(saved.getId()));
        log.info("Order marked as paid: {}", saved.getOrderCode());

        return OrderResponse.from(saved);
    }

    // ── Internal validation ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isItemInActiveOrder(Long menuItemId) {
        return orderItemRepository.existsInActiveOrder(menuItemId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("Order", id));
    }
}