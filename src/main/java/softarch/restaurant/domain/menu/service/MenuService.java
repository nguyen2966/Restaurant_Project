package softarch.restaurant.domain.menu.service;

import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuRequest;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;

import java.util.List;

/**
 * Matches the MenuService interface defined in the diagram.
 */
public interface MenuService {

    // ── Matches diagram: search(String query): List<MenuItem> ─────────────────
    List<MenuItemResponse> search(String query);

    // ── Matches diagram: filterByCategory(Long catId): List<MenuItem> ─────────
    // Category filtering is done via status in this schema (no category table)
    List<MenuItemResponse> filterByStatus(ItemStatus status);

    // ── Matches diagram: createItem / updateItem / deleteItem / setStatus ──────
    MenuItemResponse createItem(MenuRequest request);
    MenuItemResponse updateItem(Long id, MenuRequest request);
    void             deleteItem(Long id);
    MenuItemResponse setStatus(Long id, ItemStatus status);

    // ── Matches diagram: getBestSellers() ─────────────────────────────────────
    List<MenuItemResponse> getBestSellers();

    // ── Matches diagram: validateBeforeDisable(Long id) ───────────────────────
    // Throws RestaurantException if item is in an active order or active promo
    void validateBeforeDisable(Long id);

    // ── Matches diagram: validateItemsActive(menuItemIds): Boolean ────────────
    // Returns the loaded MenuItem entities (avoids second DB round-trip in OrderingFacade)
    List<MenuItem> validateItemsActive(List<Long> menuItemIds);
}