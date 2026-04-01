package softarch.restaurant.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.service.MenuService;
import softarch.restaurant.domain.order.service.OrderService;
import softarch.restaurant.domain.promotion.dto.PromoDTOs.PromoRequest;
import softarch.restaurant.domain.promotion.dto.PromoDTOs.PromoResponse;
import softarch.restaurant.domain.promotion.service.PromoService;
import softarch.restaurant.shared.exception.RestaurantException;

/**
 * AdminCatalogFacade — matches diagram exactly.
 *
 * Orchestrates UC-04 (Manage Menu) and UC-05 (Manage Promotions):
 *
 *   disableMenuItem  — guards disable with cross-domain checks:
 *     1. No active order references this item  (OrderService)
 *     2. No active promo references this item  (PromoService)
 *     → Only then allows MenuService to change status.
 *
 *   createPromotion  — validates all promo menu items are ACTIVE
 *     before creating the promotion.
 *
 * MenuController routes all status-change and delete calls through this facade.
 */
@Component
@Transactional
public class AdminCatalogFacade {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogFacade.class);

    // Matches diagram dependencies
    private final MenuService  menuService;
    private final PromoService promoService;
    private final OrderService orderService;

    public AdminCatalogFacade(MenuService menuService,
                              PromoService promoService,
                              OrderService orderService) {
        this.menuService  = menuService;
        this.promoService = promoService;
        this.orderService = orderService;
    }

    // ── disableMenuItem(Long menuItemId): boolean ─────────────────────────────
    // Matches diagram method signature

    /**
     * Safely deactivates a menu item after verifying it is not referenced
     * by any live order or active promotion.
     *
     * @return true if successfully disabled
     * @throws RestaurantException.conflict if any guard check fails
     */
    public boolean disableMenuItem(Long menuItemId) {
        log.info("AdminCatalogFacade: disabling menuItemId={}", menuItemId);

        // Guard 1 — active order check (via OrderService)
        if (orderService.isItemInActiveOrder(menuItemId)) {
            throw RestaurantException.conflict(
                "Cannot disable menu item " + menuItemId
                + ": it is currently part of one or more active orders. "
                + "Wait for those orders to complete or cancel them first.");
        }

        // Guard 2 — active promotion check (via PromoService)
        if (promoService.isItemInActivePromo(menuItemId)) {
            throw RestaurantException.conflict(
                "Cannot disable menu item " + menuItemId
                + ": it is referenced by an active promotion. "
                + "Deactivate or update the promotion first.");
        }

        // All guards passed — delegate to MenuService
        menuService.setStatus(menuItemId, ItemStatus.INACTIVE);
        log.info("AdminCatalogFacade: menuItemId={} successfully disabled", menuItemId);
        return true;
    }

    /**
     * Convenience method for MenuController DELETE endpoint.
     * Same guard logic — soft-deletes by setting INACTIVE.
     */
    public void safeDeleteMenuItem(Long menuItemId) {
        disableMenuItem(menuItemId);
    }

    /**
     * Routes status changes through the facade when disabling.
     * ACTIVE transitions are handled directly by MenuService.
     */
    public MenuItemResponse changeMenuItemStatus(Long menuItemId, ItemStatus newStatus) {
        if (newStatus == ItemStatus.INACTIVE) {
            disableMenuItem(menuItemId);
            // Return the updated state
            return menuService.filterByStatus(ItemStatus.INACTIVE).stream()
                .filter(m -> m.id().equals(menuItemId))
                .findFirst()
                .orElseThrow(() -> RestaurantException.notFound("MenuItem", menuItemId));
        }
        // For ACTIVE / OUT_OF_STOCK transitions: no cross-domain check needed
        return menuService.setStatus(menuItemId, newStatus);
    }

    // ── createPromotion(PromoRequest req): PromoResponse ─────────────────────
    // Matches diagram method signature

    /**
     * Validates that all menu items referenced in the promotion are ACTIVE
     * before persisting the promo.
     *
     * @throws RestaurantException.badRequest if any item is not ACTIVE
     */
    public PromoResponse createPromotion(PromoRequest request) {
        log.info("AdminCatalogFacade: creating promotion '{}'", request.name());

        // Validate every referenced menu item is currently ACTIVE
        if (request.menuItemIds() != null && !request.menuItemIds().isEmpty()) {
            menuService.validateItemsActive(request.menuItemIds());
        }

        PromoResponse response = promoService.createItem(request);
        log.info("AdminCatalogFacade: promotion created id={}", response.id());
        return response;
    }
}