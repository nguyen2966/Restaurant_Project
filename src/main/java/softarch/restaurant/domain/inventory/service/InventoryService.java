package softarch.restaurant.domain.inventory.service;

import softarch.restaurant.domain.inventory.dto.InventoryDTOs.AvailabilityResult;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.IngredientResponse;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.LowStockAlert;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.UsageRequest;

import java.util.List;
import java.util.Map;

public interface InventoryService {

    /**
     * Records manual ingredient usage entered by staff (e.g. end-of-shift).
     * Writes one InventoryTransaction per UsageRequest.
     */
    void recordManualUsage(List<UsageRequest> requests, Long staffUserId);

    /**
     * Called automatically when a kitchen ticket transitions to DONE.
     * Looks up the recipe for the given menu item and deducts each ingredient
     * proportionally to the quantity cooked.
     *
     * @param menuItemId the menu item that was prepared
     * @param quantity   number of portions completed
     */
    void autoDeductForMenuItem(Long menuItemId, int quantity);

    /**
     * Returns all ingredients whose current_stock <= min_threshold.
     */
    List<LowStockAlert> checkLowStockAlerts();

    /**
     * Checks whether there is sufficient stock to fulfil an order.
     * Used by OrderingFacade BEFORE creating the order.
     *
     * @param menuItemQuantities map of menuItemId → quantity requested
     * @return AvailabilityResult with detailed shortfall messages if stock is insufficient
     */
    AvailabilityResult checkAvailability(Map<Long, Integer> menuItemQuantities);

    List<IngredientResponse> getAllIngredients();
}