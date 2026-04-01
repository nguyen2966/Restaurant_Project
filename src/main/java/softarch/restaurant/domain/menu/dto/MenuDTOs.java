package softarch.restaurant.domain.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import softarch.restaurant.domain.menu.entity.ItemStatus;
import softarch.restaurant.domain.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.List;

// ── Inbound ───────────────────────────────────────────────────────────────────

public final class MenuDTOs {

    private MenuDTOs() {}

    public record MenuRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
        BigDecimal basePrice,

        String description,
        List<String> allergens
    ) {}

    public record StatusRequest(
        @NotNull(message = "Status is required")
        ItemStatus status
    ) {}

    // ── Outbound ──────────────────────────────────────────────────────────────

    public record MenuItemResponse(
        Long id,
        String name,
        BigDecimal basePrice,
        String description,
        List<String> allergens,
        ItemStatus status
    ) {
        public static MenuItemResponse from(MenuItem item) {
            return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getBasePrice(),
                item.getDescription(),
                item.getAllergens(),
                item.getStatus()
            );
        }
    }
}