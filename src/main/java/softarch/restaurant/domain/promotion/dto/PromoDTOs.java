package softarch.restaurant.domain.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import softarch.restaurant.domain.promotion.entity.PromoItem;
import softarch.restaurant.domain.promotion.entity.PromoStatus;
import softarch.restaurant.domain.promotion.entity.PromoType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class PromoDTOs {

    private PromoDTOs() {}

    // ── Inbound ───────────────────────────────────────────────────────────────

    public record PromoRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "promoType is required")
        PromoType promoType,

        String condition,

        @NotEmpty(message = "At least one menuItemId is required")
        List<Long> menuItemIds,

        LocalDateTime startDate,
        LocalDateTime dueDate,

        @NotNull(message = "discountValue is required")
        BigDecimal discountValue
    ) {}

    public record StatusRequest(
        @NotNull PromoStatus status
    ) {}

    // Matches diagram: simulatePromo() — basket to test promo against
    public record SimulateRequest(
        Map<Long, Integer> menuItemQuantities,
        BigDecimal subTotal
    ) {}

    // ── Outbound ──────────────────────────────────────────────────────────────

    public record PromoResponse(
        Long          id,
        String        name,
        PromoType     promoType,
        String        condition,
        List<Long>    menuItemIds,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        BigDecimal    discountValue,
        PromoStatus   status
    ) {
        public static PromoResponse from(PromoItem p) {
            return new PromoResponse(
                p.getId(), p.getName(), p.getPromoType(), p.getCondition(),
                p.getMenuItemIds(), p.getStartDate(), p.getDueDate(),
                p.getDiscountValue(), p.getStatus()
            );
        }
    }

    public record SimulationResult(
        BigDecimal originalSubTotal,
        BigDecimal discountAmount,
        BigDecimal finalTotal,
        String     appliedPromo
    ) {}
}