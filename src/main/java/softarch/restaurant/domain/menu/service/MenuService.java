package softarch.restaurant.domain.menu.service;

import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuRequest;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;

import java.util.List;

public interface MenuService {

    // ── Query ─────────────────────────────────────────────────────────────────

    List<MenuItemResponse> search(String query, ItemStatus status);

    MenuItemResponse getById(Long id);

    List<MenuItemResponse> getBestSellers();

    // ── Command ───────────────────────────────────────────────────────────────

    MenuItemResponse createItem(MenuRequest request);

    MenuItemResponse updateItem(Long id, MenuRequest request);

    void deleteItem(Long id);

    MenuItemResponse setStatus(Long id, ItemStatus status);

    // ── Internal validation (used by OrderingFacade & AdminCatalogFacade) ──────

    /**
     * Verifies that all given menu item IDs exist and are currently AVAILABLE.
     * Throws RestaurantException.badRequest if any item fails validation.
     *
     * @param menuItemIds list of IDs from an order request
     * @return the fully-loaded MenuItem entities (avoids a second DB round-trip)
     */
    List<MenuItem> validateItemsAvailable(List<Long> menuItemIds);

    /**
     * Returns true if the given menu item is referenced in any non-CANCELLED, non-PAID order.
     * Used by AdminCatalogFacade before disabling a menu item.
     */
    boolean isItemInActiveOrder(Long menuItemId);
}