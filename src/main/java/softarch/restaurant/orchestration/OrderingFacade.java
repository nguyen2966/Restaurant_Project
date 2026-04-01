package softarch.restaurant.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.AvailabilityResult;
import softarch.restaurant.domain.inventory.service.InventoryService;
import softarch.restaurant.domain.menu.entity.MenuItem;
import softarch.restaurant.domain.menu.service.MenuService;
import softarch.restaurant.domain.order.dto.OrderDTOs.ItemRequest;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderRequest;
import softarch.restaurant.domain.order.dto.OrderDTOs.OrderResponse;
import softarch.restaurant.domain.order.entity.Order;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.shared.exception.RestaurantException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OrderingFacade — matches diagram exactly.
 *
 * Orchestrates UC-01 (Create Order):
 *   1. Validate all requested menu items are ACTIVE        (MenuService)
 *   2. Verify sufficient stock for the full basket         (InventoryService)
 *   3. Build the Order aggregate and delegate persistence  (OrderService)
 *
 * This is the ONLY entry point for creating orders.
 * OrderController must not call OrderService.placeOrder() directly.
 */
@Component
@Transactional
public class OrderingFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderingFacade.class);

    // Matches diagram dependencies
    private final OrderService     orderService;
    private final MenuService      menuService;
    private final InventoryService inventoryService;

    public OrderingFacade(OrderService orderService,
                          MenuService menuService,
                          InventoryService inventoryService) {
        this.orderService     = orderService;
        this.menuService      = menuService;
        this.inventoryService = inventoryService;
    }

    /**
     * Matches diagram: validateAndPlaceOrder(OrderRequest req): OrderResponse
     *
     * @param request  inbound order payload (tableId, type, items)
     * @param userId   ID of the staff member creating the order (from JWT)
     */
    public OrderResponse validateAndPlaceOrder(OrderRequest request, Long userId) {
        log.info("OrderingFacade: placing order for tableId={} by userId={}",
            request.tableId(), userId);

        List<Long> menuItemIds = request.items().stream()
            .map(ItemRequest::menuItemId)
            .toList();

        // ── Step 1: Validate all items are ACTIVE ─────────────────────────────
        // MenuService.validateItemsActive() throws RestaurantException.badRequest
        // if any item is INACTIVE / OUT_OF_STOCK / not found.
        List<MenuItem> activeItems = menuService.validateItemsActive(menuItemIds);

        // Build a lookup map so we don't query the DB again per item
        Map<Long, MenuItem> itemById = activeItems.stream()
            .collect(Collectors.toMap(MenuItem::getId, m -> m));

        // ── Step 2: Check inventory availability for the full basket ──────────
        Map<Long, Integer> quantities = request.items().stream()
            .collect(Collectors.toMap(
                ItemRequest::menuItemId,
                ItemRequest::quantity,
                Integer::sum   // merge duplicate menuItemIds in the same request
            ));

        AvailabilityResult availability = inventoryService.checkAvailability(quantities);

        if (!availability.available()) {
            throw RestaurantException.unprocessable(
                "Insufficient stock for the following ingredients: "
                + String.join("; ", availability.shortfalls()));
        }

        // ── Step 3: Build the Order aggregate ─────────────────────────────────
        Order order = Order.createDraft(request.tableId(), request.type(), userId);

        for (ItemRequest itemReq : request.items()) {
            MenuItem menuItem = itemById.get(itemReq.menuItemId());

            // Allergy alert: flag item if request carries special notes and
            // the menu item has declared allergens
            boolean allergyAlert = itemReq.specialNotes() != null
                && !itemReq.specialNotes().isBlank()
                && menuItem.getAllergens() != null
                && !menuItem.getAllergens().isEmpty();

            order.addItem(
                menuItem.getId(),
                itemReq.quantity(),
                menuItem.getBasePrice(),   // snapshot price at time of order
                itemReq.specialNotes(),
                allergyAlert
            );

            // Apply item-level options if provided
            if (itemReq.options() != null && !itemReq.options().isEmpty()) {
                // Last item added is accessible via order.getItems() — set options
                order.getItems().stream()
                    .filter(oi -> oi.getMenuItemId().equals(menuItem.getId()))
                    .reduce((a, b) -> b)    // last matching item
                    .ifPresent(oi -> oi.setOptions(itemReq.options()));
            }
        }

        // ── Step 4: Persist and publish OrderPlacedEvent ──────────────────────
        // OrderService.placeOrder() calls order.confirm() (DRAFT → PLACED)
        // then publishes OrderPlacedEvent → KitchenService creates tickets.
        OrderResponse response = orderService.placeOrder(order);

        log.info("OrderingFacade: order {} created successfully (id={})",
            response.orderCode(), response.id());

        return response;
    }
}