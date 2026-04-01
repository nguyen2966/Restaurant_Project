package softarch.restaurant.domain.menu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuRequest;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;
import softarch.restaurant.domain.menu.repository.MenuRepository;
import softarch.restaurant.domain.order.repository.OrderItemRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final OrderItemRepository orderItemRepository;

    public MenuServiceImpl(MenuRepository menuRepository,
                           OrderItemRepository orderItemRepository) {
        this.menuRepository = menuRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> search(String query, ItemStatus status) {
        List<MenuItem> items;
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasStatus = status != null;

        if (hasQuery && hasStatus) {
            items = menuRepository.findByStatusAndNameContainingIgnoreCase(status, query.trim());
        } else if (hasQuery) {
            items = menuRepository.findByNameContainingIgnoreCase(query.trim());
        } else if (hasStatus) {
            items = menuRepository.findByStatus(status);
        } else {
            items = menuRepository.findAll();
        }
        return items.stream().map(MenuItemResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getById(Long id) {
        return MenuItemResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getBestSellers() {
        return menuRepository.findBestSellers().stream()
            .map(MenuItemResponse::from)
            .toList();
    }

    // ── Command ───────────────────────────────────────────────────────────────

    @Override
    public MenuItemResponse createItem(MenuRequest request) {
        MenuItem item = new MenuItem(
            request.name(),
            request.basePrice(),
            request.description(),
            request.allergens(),
            ItemStatus.AVAILABLE
        );
        return MenuItemResponse.from(menuRepository.save(item));
    }

    @Override
    public MenuItemResponse updateItem(Long id, MenuRequest request) {
        MenuItem item = findOrThrow(id);
        item.update(request.name(), request.basePrice(),
                    request.description(), request.allergens());
        return MenuItemResponse.from(menuRepository.save(item));
    }

    @Override
    public void deleteItem(Long id) {
        MenuItem item = findOrThrow(id);
        if (isItemInActiveOrder(id)) {
            throw RestaurantException.conflict(
                "Cannot delete menu item '" + item.getName() + "' — it is part of an active order.");
        }
        // Soft-delete via archiving rather than hard delete to preserve order history
        item.archive();
        menuRepository.save(item);
    }

    @Override
    public MenuItemResponse setStatus(Long id, ItemStatus status) {
        MenuItem item = findOrThrow(id);
        switch (status) {
            case AVAILABLE   -> item.activate();
            case UNAVAILABLE -> item.deactivate();
            case ARCHIVED    -> item.archive();
        }
        return MenuItemResponse.from(menuRepository.save(item));
    }

    // ── Internal validation ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> validateItemsAvailable(List<Long> menuItemIds) {
        List<MenuItem> available = menuRepository.findAvailableByIds(menuItemIds);

        Set<Long> foundIds = available.stream()
            .map(MenuItem::getId)
            .collect(Collectors.toSet());

        List<Long> unavailable = menuItemIds.stream()
            .filter(id -> !foundIds.contains(id))
            .toList();

        if (!unavailable.isEmpty()) {
            throw RestaurantException.badRequest(
                "The following menu items are unavailable or do not exist: " + unavailable);
        }
        return available;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isItemInActiveOrder(Long menuItemId) {
        return orderItemRepository.existsInActiveOrder(menuItemId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MenuItem findOrThrow(Long id) {
        return menuRepository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("MenuItem", id));
    }
}