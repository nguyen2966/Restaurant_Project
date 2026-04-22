package softarch.restaurant.domain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import softarch.restaurant.domain.inventory.entity.Ingredient;
import softarch.restaurant.domain.inventory.entity.UsageReason;

import java.math.BigDecimal;
import java.util.List;

public final class InventoryDTOs {

    private InventoryDTOs() {}

    // ── Inbound ───────────────────────────────────────────────────────────────

    public record UsageRequest(
        @NotNull(message = "ingredientId is required")
        Long ingredientId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.001", message = "amount must be positive")
        BigDecimal amount,

        @NotNull(message = "reason is required")
        UsageReason reason
    ) {}

    // ── Outbound ──────────────────────────────────────────────────────────────

    public record LowStockAlert(
        Long   ingredientId,
        String name,
        BigDecimal currentStock,
        BigDecimal minThreshold,
        String unit
    ) {
        public static LowStockAlert from(Ingredient i) {
            return new LowStockAlert(
                i.getId(),
                i.getName(),
                i.getCurrentStock(),
                i.getMinThreshold(),
                i.getUnit().name()
            );
        }
    }

    /**
     * Availability check result — returned to OrderingFacade
     * so it can surface precise messages to the caller.
     */
    public record AvailabilityResult(boolean available, List<String> shortfalls) {

        public static AvailabilityResult ok() {
            return new AvailabilityResult(true, List.of());
        }

        public static AvailabilityResult insufficient(List<String> shortfalls) {
            return new AvailabilityResult(false, shortfalls);
        }
    }


    public record IngredientResponse(
    Long id,
    String name,
    BigDecimal currentStock,
    BigDecimal minThreshold,
    String unit,
    java.time.LocalDateTime lastRestockDate
    ) {
        public static IngredientResponse from(Ingredient i) {
             return new IngredientResponse(
                    i.getId(),
                    i.getName(),
                    i.getCurrentStock(),
                    i.getMinThreshold(),
                    i.getUnit().name(),
                    i.getLastRestockDate() // Đảm bảo thực thể Ingredient có field này
                );
        }

    }
}