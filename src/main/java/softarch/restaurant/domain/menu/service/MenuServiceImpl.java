package softarch.restaurant.domain.menu.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuItemResponse;
import softarch.restaurant.domain.menu.dto.MenuDTOs.MenuRequest;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;
import softarch.restaurant.domain.menu.repository.MenuRepository;
import softarch.restaurant.domain.order.repository.OrderItemRepository;
import softarch.restaurant.domain.promotion.service.PromoService;
import softarch.restaurant.shared.exception.RestaurantException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuServiceImpl implements MenuService {

    private final MenuRepository      menuRepository;
    private final OrderItemRepository orderItemRepository;
    // @Lazy prevents circular bean dependency (PromoService → MenuService → PromoService)
    private final PromoService        promoService;

    public MenuServiceImpl(MenuRepository menuRepository,
                           OrderItemRepository orderItemRepository,
                           @Lazy PromoService promoService) {
        this.menuRepository      = menuRepository;
        this.orderItemRepository = orderItemRepository;
        this.promoService        = promoService;
    }

    // ── search(String query) ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> search(String query) {
        List<MenuItem> results = (query == null || query.isBlank())
            ? menuRepository.findAll()
            : menuRepository.findByNameContainingIgnoreCase(query.trim());
        return results.stream().map(MenuItemResponse::from).toList();
    }

    // ── filterByCategory / filterByStatus ────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> filterByStatus(ItemStatus status) {
        return menuRepository.findByStatus(status)
            .stream().map(MenuItemResponse::from).toList();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public MenuItemResponse createItem(MenuRequest request) {
        MenuItem item = new MenuItem(
            request.name(), request.basePrice(),
            request.description(), request.allergens()
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
        // validateBeforeDisable checks active orders and promos
        validateBeforeDisable(id);
        // Soft-delete: set INACTIVE rather than hard delete (preserves order history)
        item.setStatus(ItemStatus.INACTIVE);
        menuRepository.save(item);
    }

    @Override
    public MenuItemResponse setStatus(Long id, ItemStatus status) {
        MenuItem item = findOrThrow(id);
        if (status == ItemStatus.INACTIVE) {
            validateBeforeDisable(id);
        }
        item.setStatus(status);
        return MenuItemResponse.from(menuRepository.save(item));
    }

    // ── getBestSellers() ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getBestSellers() {
        return menuRepository.findBestSellers()
            .stream().map(MenuItemResponse::from).toList();
    }

    // ── validateBeforeDisable(Long id) ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void validateBeforeDisable(Long id) {
        MenuItem item = findOrThrow(id);

        if (orderItemRepository.existsInActiveOrder(id)) {
            throw RestaurantException.conflict(
                "Cannot disable '" + item.getName() + "': it is part of an active order.");
        }
        if (promoService.isItemInActivePromo(id)) {
            throw RestaurantException.conflict(
                "Cannot disable '" + item.getName() + "': it is referenced by an active promotion.");
        }
    }

    // ── validateItemsActive(List<Long> menuItemIds): Boolean ─────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> validateItemsActive(List<Long> menuItemIds) {
        List<MenuItem> active = menuRepository.findActiveByIds(menuItemIds);

        Set<Long> activeIds = active.stream()
            .map(MenuItem::getId)
            .collect(Collectors.toSet());

        List<Long> inactive = menuItemIds.stream()
            .filter(id -> !activeIds.contains(id))
            .toList();

        if (!inactive.isEmpty()) {
            throw RestaurantException.badRequest(
                "The following menu items are not ACTIVE or do not exist: " + inactive);
        }
        return active;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MenuItem findOrThrow(Long id) {
        return menuRepository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("MenuItem", id));
    }
}