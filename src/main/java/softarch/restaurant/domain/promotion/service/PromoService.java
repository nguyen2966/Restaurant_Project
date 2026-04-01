package softarch.restaurant.domain.promotion.service;

import softarch.restaurant.domain.promotion.dto.PromoDTOs.*;
import softarch.restaurant.domain.promotion.entity.PromoStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PromoService {

    // Matches diagram: createItem / updateItem / deleteItem / setStatus
    PromoResponse createItem(PromoRequest request);
    PromoResponse updateItem(Long id, PromoRequest request);
    void          deleteItem(Long id);
    PromoResponse setStatus(Long id, PromoStatus status);

    // Matches diagram: simulatePromo() — preview discount for a basket
    SimulationResult simulatePromo(SimulateRequest request);

    /**
     * Matches diagram: calculateDiscount(menuItemQuantities, subTotal): Double
     * Core pricing logic — called by CheckoutFacade to compute the final bill.
     *
     * @param menuItemQuantities map of menuItemId → quantity in the order
     * @param subTotal           raw subtotal before discount
     * @return total discount amount to subtract
     */
    BigDecimal calculateDiscount(Map<Long, Integer> menuItemQuantities, BigDecimal subTotal);

    // Matches diagram: isItemInActivePromo(menuItemId): Boolean
    boolean isItemInActivePromo(Long menuItemId);

    List<PromoResponse> findByStatus(PromoStatus status);
}