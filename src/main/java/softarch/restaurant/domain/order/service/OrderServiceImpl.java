package softarch.restaurant.domain.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.dto.SaleData;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.entity.OrderItem;
import softarch.restaurant.domain.order.entity.OrderStatus;
import softarch.restaurant.domain.order.event.OrderPaidEvent;
import softarch.restaurant.domain.order.event.OrderPlacedEvent;
import softarch.restaurant.domain.order.repository.OrderItemRepository;
import softarch.restaurant.domain.order.repository.OrderRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository       orderRepository;
    private final OrderItemRepository   orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository     = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher      = eventPublisher;
    }

    // ── Core placement (called by OrderingFacade) ──────────────────────────────

    @Override
    public OrderResponse placeOrder(Order order) {
        order.confirm();
        Order saved = orderRepository.save(order);
        log.info("Order placed: {} (id={})", saved.getOrderCode(), saved.getId());
        eventPublisher.publishEvent(new OrderPlacedEvent(saved.getId(), saved.getItems()));
        return OrderResponse.from(saved);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

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

    // ── Commands ───────────────────────────────────────────────────────────────

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

    // ── Validation ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isItemInActiveOrder(Long menuItemId) {
        return orderItemRepository.existsInActiveOrder(menuItemId);
    }

    // ── Analytics: getSalesData(startDate, endDate) ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SaleData> getSalesData(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> paidOrders = orderRepository.findByCreatedAtBetween(startDate, endDate)
            .stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID)
            .toList();

        // Aggregate quantity + revenue per menuItemId
        Map<Long, long[]>       quantities = new LinkedHashMap<>();
        Map<Long, BigDecimal>   revenues   = new LinkedHashMap<>();

        for (Order order : paidOrders) {
            for (OrderItem item : order.getItems()) {
                Long mid = item.getMenuItemId();
                quantities.computeIfAbsent(mid, k -> new long[]{0})[0] += item.getQuantity();
                revenues.merge(mid, item.lineTotal(), BigDecimal::add);
            }
        }

        return quantities.entrySet().stream()
            .map(e -> new SaleData(
                e.getKey(),
                "Item #" + e.getKey(),   // name resolved by MenuService if needed
                e.getValue()[0],
                revenues.getOrDefault(e.getKey(), BigDecimal.ZERO)
            ))
            .toList();
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("Order", id));
    }
}
