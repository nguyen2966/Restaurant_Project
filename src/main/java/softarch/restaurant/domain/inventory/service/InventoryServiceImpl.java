package softarch.restaurant.domain.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.AvailabilityResult;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.LowStockAlert;
import softarch.restaurant.domain.inventory.dto.InventoryDTOs.UsageRequest;
import softarch.restaurant.domain.inventory.entity.Ingredient;
import softarch.restaurant.domain.inventory.entity.InventoryTransaction;
import softarch.restaurant.domain.inventory.entity.RecipeItem;
import softarch.restaurant.domain.inventory.entity.UsageReason;
import softarch.restaurant.domain.inventory.repository.IngredientRepository;
import softarch.restaurant.domain.inventory.repository.RecipeRepository;
import softarch.restaurant.domain.inventory.repository.TransactionRepository;
import softarch.restaurant.shared.exception.RestaurantException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository     recipeRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryServiceImpl(IngredientRepository ingredientRepository,
                                RecipeRepository recipeRepository,
                                TransactionRepository transactionRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.ingredientRepository  = ingredientRepository;
        this.recipeRepository      = recipeRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher        = eventPublisher;
    }

    // ── Manual usage ──────────────────────────────────────────────────────────

    @Override
    public void recordManualUsage(List<UsageRequest> requests, Long staffUserId) {
        for (UsageRequest req : requests) {
            Ingredient ingredient = findIngredientOrThrow(req.ingredientId());
            ingredient.deduct(req.amount());
            ingredientRepository.save(ingredient);

            transactionRepository.save(new InventoryTransaction(
                req.ingredientId(),
                req.amount().negate(),
                req.reason(),
                staffUserId
            ));
        }
        // After deductions, log any newly low-stock ingredients
        logLowStockWarnings();
    }

    // ── Auto-deduct (triggered by kitchen ticket DONE event) ──────────────────

    @Override
    public void autoDeductForMenuItem(Long menuItemId, int quantity) {
        List<RecipeItem> recipe = recipeRepository.findByMenuItemId(menuItemId);

        if (recipe.isEmpty()) {
            log.warn("No recipe found for menuItemId={}. Skipping inventory deduction.", menuItemId);
            return;
        }

        for (RecipeItem recipeItem : recipe) {
            BigDecimal totalNeeded = recipeItem.getRequiredAmount()
                .multiply(BigDecimal.valueOf(quantity));

            Ingredient ingredient = findIngredientOrThrow(recipeItem.getIngredientId());
            ingredient.deduct(totalNeeded);
            ingredientRepository.save(ingredient);

            transactionRepository.save(new InventoryTransaction(
                recipeItem.getIngredientId(),
                totalNeeded.negate(),
                UsageReason.AUTO_DEDUCT,
                null   // system-initiated — no user
            ));
        }
        logLowStockWarnings();
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LowStockAlert> checkLowStockAlerts() {
        return ingredientRepository.findLowStockIngredients()
            .stream()
            .map(LowStockAlert::from)
            .toList();
    }

    // ── Pre-order availability check ──────────────────────────────────────────

    /**
     * For each requested menu item:
     *   1. Load its recipe (ingredient → requiredAmount)
     *   2. Multiply requiredAmount × quantity
     *   3. Compare against current stock
     *
     * Aggregates across multiple items so that two dishes sharing the same
     * ingredient are checked together (e.g. 2× Phở + 1× Bún both use beef).
     */
    @Override
    @Transactional(readOnly = true)
    public AvailabilityResult checkAvailability(Map<Long, Integer> menuItemQuantities) {

        // Aggregate total ingredient demand: ingredientId → totalNeeded
        Map<Long, BigDecimal> totalDemand = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : menuItemQuantities.entrySet()) {
            Long menuItemId  = entry.getKey();
            int  quantity    = entry.getValue();

            List<RecipeItem> recipe = recipeRepository.findByMenuItemId(menuItemId);
            for (RecipeItem ri : recipe) {
                BigDecimal needed = ri.getRequiredAmount()
                    .multiply(BigDecimal.valueOf(quantity));
                totalDemand.merge(ri.getIngredientId(), needed, BigDecimal::add);
            }
        }

        if (totalDemand.isEmpty()) {
            // No recipes defined → assume items without recipes are always available
            return AvailabilityResult.ok();
        }

        // Load all relevant ingredients in one query
        List<Ingredient> ingredients = ingredientRepository.findAllById(totalDemand.keySet());
        Map<Long, Ingredient> ingredientMap = ingredients.stream()
            .collect(Collectors.toMap(Ingredient::getId, i -> i));

        List<String> shortfalls = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> demand : totalDemand.entrySet()) {
            Long       ingredientId = demand.getKey();
            BigDecimal needed       = demand.getValue();
            Ingredient ingredient   = ingredientMap.get(ingredientId);

            if (ingredient == null) {
                shortfalls.add("Ingredient id=" + ingredientId + " not found in inventory");
                continue;
            }
            if (ingredient.getCurrentStock().compareTo(needed) < 0) {
                shortfalls.add(String.format(
                    "'%s': needed %.3f %s, available %.3f %s",
                    ingredient.getName(),
                    needed, ingredient.getUnit().name(),
                    ingredient.getCurrentStock(), ingredient.getUnit().name()
                ));
            }
        }

        return shortfalls.isEmpty()
            ? AvailabilityResult.ok()
            : AvailabilityResult.insufficient(shortfalls);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Ingredient findIngredientOrThrow(Long id) {
        return ingredientRepository.findById(id)
            .orElseThrow(() -> RestaurantException.notFound("Ingredient", id));
    }

    private void logLowStockWarnings() {
        List<Ingredient> lowStock = ingredientRepository.findLowStockIngredients();
        if (!lowStock.isEmpty()) {
            lowStock.forEach(i ->
                log.warn("LOW STOCK — '{}': {}/{} {}",
                    i.getName(), i.getCurrentStock(), i.getMinThreshold(), i.getUnit()));
        }
    }
}