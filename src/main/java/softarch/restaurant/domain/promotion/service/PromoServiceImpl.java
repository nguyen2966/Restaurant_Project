package softarch.restaurant.domain.promotion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.promotion.dto.PromoDTOs.*;
import softarch.restaurant.domain.promotion.entity.PromoItem;
import softarch.restaurant.domain.promotion.entity.PromoStatus;
import softarch.restaurant.domain.promotion.repository.PromoRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PromoServiceImpl implements PromoService {

    private final PromoRepository repository;

    public PromoServiceImpl(PromoRepository repository) {
        this.repository = repository;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public PromoResponse createItem(PromoRequest req) {
        PromoItem item = new PromoItem(req.name(), req.promoType(), req.condition(),
            req.menuItemIds(), req.startDate(), req.dueDate(), req.discountValue());
        return PromoResponse.from(repository.save(item));
    }

    @Override
    public PromoResponse updateItem(Long id, PromoRequest req) {
        PromoItem item = findOrThrow(id);
        item.update(req.name(), req.promoType(), req.condition(), req.menuItemIds(),
                    req.startDate(), req.dueDate(), req.discountValue());
        return PromoResponse.from(repository.save(item));
    }

    @Override
    public void deleteItem(Long id) {
        PromoItem item = findOrThrow(id);
        item.setStatus(PromoStatus.INACTIVE);
        repository.save(item);
    }

    @Override
    public PromoResponse setStatus(Long id, PromoStatus status) {
        PromoItem item = findOrThrow(id);
        item.setStatus(status);
        return PromoResponse.from(repository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromoResponse> findByStatus(PromoStatus status) {
        return repository.findByStatus(status).stream().map(PromoResponse::from).toList();
    }

    // ── simulatePromo() ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SimulationResult simulatePromo(SimulateRequest req) {
        BigDecimal discount = calculateDiscount(req.menuItemQuantities(), req.subTotal());
        BigDecimal finalTotal = req.subTotal().subtract(discount).max(BigDecimal.ZERO);

        // Find which promo was applied (first matching active one)
        String appliedName = repository.findByStatus(PromoStatus.ACTIVE).stream()
            .filter(PromoItem::isActive)
            .filter(p -> req.menuItemQuantities().keySet().stream()
                            .anyMatch(p::coversMenuItem))
            .map(PromoItem::getName)
            .findFirst()
            .orElse("No promotion applied");

        return new SimulationResult(req.subTotal(), discount, finalTotal, appliedName);
    }

    // ── calculateDiscount(menuItemQuantities, subTotal) ───────────────────────

    /**
     * Iterates all ACTIVE promos and applies matching ones.
     * Strategy per PromoType:
     *   BY_PERCENT   → subTotal × (discountValue / 100)
     *   BY_AMOUNT    → fixed discountValue off subTotal
     *   COMBO        → discountValue if ALL combo items are present
     *   BUY_X_GET_Y  → discountValue per qualifying set (condition encodes X)
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Map<Long, Integer> menuItemQuantities, BigDecimal subTotal) {
        List<PromoItem> activePromos = repository.findByStatus(PromoStatus.ACTIVE).stream()
            .filter(PromoItem::isActive)
            .toList();

        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (PromoItem promo : activePromos) {
            boolean anyItemMatches = menuItemQuantities.keySet().stream()
                .anyMatch(promo::coversMenuItem);
            if (!anyItemMatches) continue;

            BigDecimal promoDiscount = switch (promo.getPromoType()) {
                case BY_PERCENT -> subTotal
                    .multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                case BY_AMOUNT -> promo.getDiscountValue()
                    .min(subTotal); // never exceed the subtotal

                case COMBO -> {
                    // All items in the combo must be present in the order
                    boolean allPresent = promo.getMenuItemIds().stream()
                        .allMatch(menuItemQuantities::containsKey);
                    yield allPresent ? promo.getDiscountValue() : BigDecimal.ZERO;
                }

                case BUY_X_GET_Y -> {
                    // condition encodes "X" (buy X items, get discount)
                    // Simplified: count total qualifying items and apply discount per set
                    long qualifyingQty = menuItemQuantities.entrySet().stream()
                        .filter(e -> promo.getMenuItemIds().contains(e.getKey()))
                        .mapToLong(Map.Entry::getValue)
                        .sum();
                    int buyX = parseBuyX(promo.getCondition());
                    long sets = qualifyingQty / (buyX + 1);
                    yield promo.getDiscountValue().multiply(BigDecimal.valueOf(sets));
                }
            };

            totalDiscount = totalDiscount.add(promoDiscount);
        }

        // Discount can never exceed the subtotal
        return totalDiscount.min(subTotal).setScale(2, RoundingMode.HALF_UP);
    }

    // ── isItemInActivePromo(menuItemId) ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isItemInActivePromo(Long menuItemId) {
        return !repository.findActiveByMenuItemId(menuItemId).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PromoItem findOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("PromoItem", id));
    }

    /** Parses "buyX=2" style condition string. Defaults to 1 if unparseable. */
    private int parseBuyX(String condition) {
        if (condition == null) return 1;
        try {
            String[] parts = condition.split("=");
            return Integer.parseInt(parts[parts.length - 1].trim());
        } catch (Exception e) {
            return 1;
        }
    }
}